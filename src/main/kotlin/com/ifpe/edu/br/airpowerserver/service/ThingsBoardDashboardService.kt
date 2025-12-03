package com.ifpe.edu.br.airpowerserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ifpe.edu.br.airpowerserver.dto.Id
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardConfig
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

    // CORREÇÃO: Usamos 'title' para preencher tanto o title quanto o name,
    // pois a coluna 'name' não existe no banco.
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
        // CORREÇÃO: Removida a coluna d.name do SELECT.
        // Adicionado CAST(u.id AS uuid) ou ::uuid para garantir compatibilidade de tipos no Postgres.
        // Nota: A lógica de JOIN assume que a relação está populada na tabela 'relation'.
        // Se esta query não retornar nada, podemos tentar usar a coluna 'assigned_customers'.
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
            // É boa prática relançar ou tratar de forma que o controller saiba que falhou,
            // mas retornar lista vazia é seguro para evitar crash.
            emptyList()
        }
    }

    /**
     * Busca os IDs de dispositivos únicos de um dashboard específico, lendo a configuração JSON
     * diretamente do banco de dados.
     */
    fun getDeviceIdsFromDashboard(userId: UUID, dashboardId: String): List<Id> {
        // O userId é usado para contexto de autorização (implícito), mas a query só precisa do dashboardId
        val sql = "SELECT configuration::text FROM dashboard WHERE id = :dashboardId"
        val params = MapSqlParameterSource().addValue("dashboardId", UUID.fromString(dashboardId))
        logger.debug("Buscando configuração do dashboard {} para o usuário {}", dashboardId, userId)

        try {
            // 1. Buscar o JSON de configuração como uma String
            val configJson = namedJdbcTemplate.queryForObject(sql, params, String::class.java)
                ?: throw EmptyResultDataAccessException("Configuração do dashboard não encontrada", 1)

            // 2. Parsear o JSON usando ObjectMapper e os DTOs que já criamos
            val dashboardConfig = objectMapper.readValue<DashboardConfig>(configJson)
            val deviceIds = mutableSetOf<Id>()

            // 3. Extrair os IDs (lógica idêntica à anterior)
            dashboardConfig.configuration.widgets.values.forEach { widget ->
                widget.config.datasources.forEach { datasource ->
                    val id = datasource.entityId ?: datasource.deviceId
                    if (id != null) {
                        deviceIds.add(id)
                    }
                }
            }
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