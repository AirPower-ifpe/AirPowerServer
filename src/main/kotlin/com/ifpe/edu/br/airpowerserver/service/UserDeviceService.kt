package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.DeviceSummaryDTO
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.Instant
import java.util.*

data class UserInfo(val tenantId: UUID, val customerId: UUID?)

@Service
class UserDeviceService(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(UserDeviceService::class.java)

    private val userInfoRowMapper = RowMapper<UserInfo> { rs: ResultSet, _: Int ->
        val customerIdStr = rs.getString("customer_id")
        UserInfo(
            tenantId = UUID.fromString(rs.getString("tenant_id")),
            customerId = if (customerIdStr != null) UUID.fromString(customerIdStr) else null
        )
    }

    /**
     * @param userIdStr O ID do usuário no formato String.
     * @param activityTimeoutSeconds O tempo em segundos que um dispositivo é considerado ativo desde sua última atividade.
     * O padrão é 60 segundos (1 minuto).
     * @return Uma lista de DeviceSummaryDTO.
     */
    fun getDeviceSummariesForUser(
        userIdStr: String,
        activityTimeoutSeconds: Long = 1200L
    ): List<DeviceSummaryDTO> {
        val userUuid: UUID = UUID.fromString(userIdStr)
        val userInfoSql = "SELECT tenant_id, customer_id FROM tb_user WHERE id = ?"
        val userInfo: UserInfo = try {
            jdbcTemplate.queryForObject(userInfoSql, userInfoRowMapper, userUuid)!!
        } catch (e: EmptyResultDataAccessException) {
            logger.warn("Usuário não encontrado com ID: {}", userUuid)
            throw IllegalStateException("Informações do usuário não encontradas para ID: $userUuid")
        } catch (e: Exception) {
            logger.error("Erro ao buscar informações do usuário com ID: {}", userUuid, e)
            throw IllegalStateException("Erro ao buscar informações do usuário com ID: $userUuid")
        }

        val activityTimeoutMs = activityTimeoutSeconds * 1000L

        val deviceSummaryRowMapper = RowMapper<DeviceSummaryDTO> { rs: ResultSet, _: Int ->
            val isActiveFromTb = rs.getObject("is_active") as? Boolean
            val lastActivityTimestampMs = rs.getLong("last_activity_ts")
            val wasNullLat = rs.wasNull()

            val isActive = isActiveFromTb
                ?: if (!wasNullLat && lastActivityTimestampMs > 0) {
                    val currentTimeMs = Instant.now().toEpochMilli()
                    (currentTimeMs - lastActivityTimestampMs) <= activityTimeoutMs
                } else {
                    false
                }

            DeviceSummaryDTO(
                id = UUID.fromString(rs.getString("device_id")),
                name = rs.getString("device_name"),
                label = rs.getString("device_label"),
                type = rs.getString("device_type"),
                isActive = isActive
            )
        }

        var devicesSql = """
            SELECT 
                d.id AS device_id, 
                d.name AS device_name, 
                d.label AS device_label, 
                d.type AS device_type,
                attr_active.bool_v AS is_active,
                attr_lat.long_v AS last_activity_ts 
            FROM 
                device d
            LEFT JOIN key_dictionary kd_active ON kd_active.key = 'active'
            LEFT JOIN attribute_kv attr_active ON d.id = attr_active.entity_id 
                AND attr_active.attribute_key = kd_active.key_id
            LEFT JOIN key_dictionary kd_lat ON kd_lat.key = 'lastActivityTime'
            LEFT JOIN attribute_kv attr_lat ON d.id = attr_lat.entity_id 
                AND attr_lat.attribute_key = kd_lat.key_id
            WHERE 
                d.tenant_id = ?
        """.trimIndent()

        val queryParams = mutableListOf<Any>()
        queryParams.add(userInfo.tenantId)

        if (userInfo.customerId != null) {
            devicesSql += " AND d.customer_id = ?"
            queryParams.add(userInfo.customerId)
        } else {
            devicesSql += " AND d.customer_id IS NULL"
        }

        devicesSql += " ORDER BY d.name"

        val resultSet = jdbcTemplate.query(devicesSql, deviceSummaryRowMapper, *queryParams.toTypedArray())
        logger.info("Busca concluída. Foram encontrados {} dispositivos para o usuário {}", resultSet.size, userUuid)
        return resultSet
    }
}