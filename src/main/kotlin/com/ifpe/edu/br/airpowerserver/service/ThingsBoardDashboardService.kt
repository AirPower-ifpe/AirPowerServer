package com.ifpe.edu.br.airpowerserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ifpe.edu.br.airpowerserver.dto.Id
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardConfiguration
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardInfo
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.util.*

/**
 * Service class for handling business logic related to ThingsBoard dashboards.
 *
 * This service interacts directly with the ThingsBoard database to retrieve dashboard
 * and device information, bypassing the ThingsBoard API for performance.
 *
 * @property namedJdbcTemplate The template for executing named parameter SQL queries.
 * @property objectMapper The mapper for handling JSON serialization and deserialization.
 */
@Service
class ThingsBoardDashboardService(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ThingsBoardDashboardService::class.java)

    private val dashboardInfoRowMapper = RowMapper<DashboardInfo> { rs: ResultSet, _: Int ->
        val title = rs.getString("title")
        DashboardInfo(
            id = Id(
                entityType = "DASHBOARD",
                id = UUID.fromString(rs.getString("id"))
            ),
            name = title,
            title = title
        )
    }

    /**
     * Retrieves all dashboards assigned to a specific user.
     *
     * This method queries the database to find dashboards related to the user's customer entity
     * and then populates each dashboard with its associated device IDs.
     *
     * @param userId The UUID of the user.
     * @return A list of [DashboardInfo] objects, each containing details about a dashboard
     *         and the IDs of devices associated with it. Returns an empty list if an error occurs.
     */
    fun getDashboardsForUser(userId: UUID): List<DashboardInfo> {
        val sql = """
            SELECT d.id, d.title
            FROM dashboard d
            JOIN relation r ON d.id = r.to_id
            JOIN tb_user u ON r.from_id = u.customer_id
            WHERE u.id = :userId
              AND r.relation_type_group = 'DASHBOARD'
              AND r.from_type = 'CUSTOMER'
              AND r.to_type = 'DASHBOARD'
        """.trimIndent()

        val params = MapSqlParameterSource().addValue("userId", userId)
        logger.debug("Buscando dashboards no DB para o usuário: {}", userId)

        return try {
            val dashboards = namedJdbcTemplate.query(sql, params, dashboardInfoRowMapper)
            dashboards.forEach { dashboard ->
                dashboard.devicesIds = getDeviceIdsFromDashboard(dashboard.id.id.toString())
                    .map { it.id.toString() }
            }
            dashboards
        } catch (e: Exception) {
            logger.error("Erro ao buscar dashboards diretamente do DB para o usuário {}: {}", userId, e.message)
            emptyList()
        }
    }

    /**
     * Extracts all unique device IDs from a specific dashboard's configuration.
     *
     * The method parses the dashboard's configuration JSON to find device aliases,
     * which can be defined as a static list ('entityList') or a dynamic query ('relationsQuery').
     *
     * @param dashboardId The UUID of the dashboard as a String.
     * @return A list of unique [Id] objects for the devices found in the dashboard.
     *         Returns an empty list if the dashboard is not found or an error occurs.
     */
    fun getDeviceIdsFromDashboard(dashboardId: String): List<Id> {
        val sqlConfig = "SELECT configuration::text FROM dashboard WHERE id = :dashboardId"
        val params = MapSqlParameterSource().addValue("dashboardId", UUID.fromString(dashboardId))

        try {
            val configJson = namedJdbcTemplate.queryForObject(sqlConfig, params, String::class.java)
                ?: throw EmptyResultDataAccessException(1)

            val dashboardConfig = objectMapper.readValue<DashboardConfiguration>(configJson)
            val deviceIds = mutableSetOf<Id>()

            dashboardConfig.entityAliases?.values?.forEach { alias ->
                val filter = alias.filter ?: return@forEach

                when (filter.type) {
                    "entityList" -> {
                        filter.entityList?.forEach { idStr ->
                            try {
                                deviceIds.add(Id(entityType = "DEVICE", id = UUID.fromString(idStr)))
                            } catch (e: Exception) { logger.warn("UUID inválido na lista: $idStr") }
                        }
                    }
                    "relationsQuery" -> {
                        val rootId = filter.rootEntity?.id
                        val direction = filter.direction
                        val relationType = filter.relationType
                        if (rootId != null && direction != null) {
                            val relations = findRelatedDevices(
                                UUID.fromString(rootId),
                                direction,
                                relationType
                            )
                            deviceIds.addAll(relations)
                        }
                    }
                }
            }
            return deviceIds.toList()
        } catch (e: Exception) {
            logger.error("Erro ao resolver dashboard {}: {}", dashboardId, e.message)
            return emptyList()
        }
    }

    /**
     * Recursively finds related devices in the ThingsBoard 'relation' table.
     *
     * This method performs a recursive SQL query to traverse the entity hierarchy
     * (e.g., Asset -> Asset -> Device) and find all descendant devices.
     *
     * @param rootEntityId The starting UUID for the recursive search.
     * @param direction The direction of the relationship ('FROM' or 'TO').
     * @param relationType The specific type of relation to follow (e.g., 'Contains').
     * @return A list of [Id] objects representing the found devices.
     */
    private fun findRelatedDevices(rootEntityId: UUID, direction: String, relationType: String?): List<Id> {
        val isFrom = direction.equals("FROM", ignoreCase = true)

        val parentColumn = if (isFrom) "from_id" else "to_id"
        val childColumn = if (isFrom) "to_id" else "from_id"
        val childTypeColumn = if (isFrom) "to_type" else "from_type"

        val sql = """
            WITH RECURSIVE entity_tree AS (
                SELECT 
                    $childColumn as entity_id, 
                    $childTypeColumn as entity_type
                FROM relation 
                WHERE $parentColumn = :rootId
                  AND relation_type_group = 'COMMON'
                  ${if (!relationType.isNullOrBlank()) "AND relation_type = :relType" else ""}
                
                UNION ALL
                
                SELECT 
                    r.$childColumn, 
                    r.$childTypeColumn
                FROM relation r
                INNER JOIN entity_tree et ON r.$parentColumn = et.entity_id
                WHERE r.relation_type_group = 'COMMON'
                  ${if (!relationType.isNullOrBlank()) "AND r.relation_type = :relType" else ""}
            )
            SELECT entity_id 
            FROM entity_tree 
            WHERE entity_type = 'DEVICE'
        """

        val params = MapSqlParameterSource().addValue("rootId", rootEntityId)

        if (!relationType.isNullOrBlank()) {
            params.addValue("relType", relationType)
        }

        return namedJdbcTemplate.query(sql, params) { rs, _ ->
            Id(entityType = "DEVICE", id = UUID.fromString(rs.getString("entity_id")))
        }
    }
}