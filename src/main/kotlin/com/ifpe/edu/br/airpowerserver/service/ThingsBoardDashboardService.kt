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
    fun getDeviceIdsFromDashboard(userId: UUID, dashboardId: String): List<Id> {
        val sql = "SELECT configuration::text FROM dashboard WHERE id = :dashboardId"
        val params = MapSqlParameterSource().addValue("dashboardId", UUID.fromString(dashboardId))
        logger.debug("Buscando configuração do dashboard {} para o usuário {}", dashboardId, userId)

        try {
            val configJson = namedJdbcTemplate.queryForObject(sql, params, String::class.java)
                ?: throw EmptyResultDataAccessException("Configuração do dashboard não encontrada", 1)
            val dashboardConfiguration = objectMapper.readValue<DashboardConfiguration>(configJson)
            val deviceIds = mutableSetOf<Id>()
            dashboardConfiguration.entityAliases?.values?.forEach { alias ->
                if (alias.filter?.type == "singleEntity" &&
                    alias.filter.singleEntity?.entityType == "DEVICE") {

                    try {
                        val uuid = UUID.fromString(alias.filter.singleEntity.id)
                        deviceIds.add(Id(entityType = "DEVICE", id = uuid))
                    } catch (e: Exception) {
                        logger.warn("UUID inválido no Alias: {}", alias.filter.singleEntity.id)
                    }
                }
            }
            dashboardConfiguration.widgets?.values?.forEach { widget ->
                widget.config.datasources.forEach { datasource ->
                    val idString = datasource.entityId ?: datasource.deviceId
                    if (idString != null) {
                        try {
                            deviceIds.add(Id(entityType = "DEVICE", id = UUID.fromString(idString.id.toString())))
                        } catch (e: Exception) {
                            logger.warn("UUID inválido encontrado no widget do dashboard {}: {}", dashboardId, idString)
                        }
                    }
                }
            }
            logger.info("Encontrados {} dispositivos no dashboard {}", deviceIds.size, dashboardId)
            return deviceIds.toList()
        } catch (e: EmptyResultDataAccessException) {
            logger.warn("Nenhum dashboard encontrado no DB com o ID: {} para usuário {}", dashboardId, userId)
            return emptyList()
        } catch (e: Exception) {
            logger.error("Falha ao parsear a configuração do dashboard {} (do DB): {}", dashboardId, e.message)
            return emptyList()
        }
    }
}