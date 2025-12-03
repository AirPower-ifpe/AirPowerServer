package com.ifpe.edu.br.airpowerserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ifpe.edu.br.airpowerserver.dto.Id
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardConfig
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

    fun getDashboardsForUser(userId: UUID): List<DashboardInfo> {
        // Nota: A lógica de JOIN assume que a relação está populada na tabela 'relation'.
        // Se esta query não retornar nada, devemos usar a coluna 'assigned_customers'.
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
            namedJdbcTemplate.query(sql, params, dashboardInfoRowMapper)
        } catch (e: Exception) {
            logger.error("Erro ao buscar dashboards diretamente do DB para o usuário {}: {}", userId, e.message)
            emptyList()
        }
    }

    /**
     * Busca os IDs de dispositivos únicos de um dashboard específico.
     */
    fun getDeviceIdsFromDashboard(customerId: UUID, dashboardId: String): List<Id> {
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
                    // ---------------------------------------------------------
                    // OPÇÃO 1: Lista Manual (Entity List)
                    // Útil para grupos pequenos e fixos (ex: A e B)
                    // Configurado na GUI como Filter Type: "Entity List"
                    // ---------------------------------------------------------
                    "entityList" -> {
                        filter.entityList?.forEach { idStr ->
                            try {
                                deviceIds.add(Id(entityType = "DEVICE", id = UUID.fromString(idStr)))
                            } catch (e: Exception) { logger.warn("UUID inválido na lista: $idStr") }
                        }
                    }

                    // ---------------------------------------------------------
                    // OPÇÃO 2: Relations Query (A solução robusta)
                    // Busca devices conectados a um Asset específico.
                    // ---------------------------------------------------------
                    "relationsQuery" -> {
                        val rootId = filter.rootEntity?.id
                        val direction = filter.direction // FROM ou TO
                        // Se o usuário não definir relationType na GUI, o TB às vezes omite ou usa "Contains"
                        val relationType = filter.relationType

                        if (rootId != null && direction != null) {
                            // Busca dinâmica na tabela de relações
                            val relations = findRelatedDevices(
                                UUID.fromString(rootId),
                                direction,
                                relationType
                            )
                            deviceIds.addAll(relations)
                        }
                    }

                    // ... outros casos (singleEntity, etc) ...
                }
            }

            return deviceIds.toList()

        } catch (e: Exception) {
            logger.error("Erro ao resolver dashboard {}: {}", dashboardId, e.message)
            return emptyList()
        }
    }

    /**
     * Busca na tabela 'relation' do ThingsBoard.
     * Estrutura da tabela: from_id, from_type, to_id, to_type, relation_type_group, relation_type
     */
    private fun findRelatedDevices(rootEntityId: UUID, direction: String, relationType: String?): List<Id> {
        // Se direction é FROM, o Asset é o 'from_id' e buscamos os 'to_id' (os devices)
        // Se direction é TO, o Asset é o 'to_id' (menos comum para agrupamento)

        val isFrom = direction.equals("FROM", ignoreCase = true)

        val selectColumn = if (isFrom) "to_id" else "from_id"
        val whereColumn = if (isFrom) "from_id" else "to_id"
        // Garantimos que só retornamos dispositivos
        val targetTypeColumn = if (isFrom) "to_type" else "from_type"

        var sql = """
            SELECT $selectColumn as device_id 
            FROM relation 
            WHERE $whereColumn = :rootId 
              AND $targetTypeColumn = 'DEVICE'
              AND relation_type_group = 'COMMON'
        """

        val params = MapSqlParameterSource().addValue("rootId", rootEntityId)

        // Adiciona filtro por tipo de relação se especificado (ex: 'Contains')
        if (!relationType.isNullOrBlank()) {
            sql += " AND relation_type = :relType"
            params.addValue("relType", relationType)
        }

        return namedJdbcTemplate.query(sql, params) { rs, _ ->
            Id(entityType = "DEVICE", id = UUID.fromString(rs.getString("device_id")))
        }
    }

}