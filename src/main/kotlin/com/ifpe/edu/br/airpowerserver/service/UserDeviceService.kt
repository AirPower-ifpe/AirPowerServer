package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.DeviceSummaryDTO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
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

    private val deviceSummaryRowMapper = RowMapper<DeviceSummaryDTO> { rs: ResultSet, _: Int ->
        val lastActivityTimestampMs = rs.getLong("last_activity_ts")
        val wasNull = rs.wasNull()

        var isActive = false
        val tenSecondsInMillis = 10_000L

        if (!wasNull && lastActivityTimestampMs > 0) {
            val currentTimeMs = Instant.now().toEpochMilli()
            if ((currentTimeMs - lastActivityTimestampMs) <= tenSecondsInMillis) {
                isActive = true
            }
        } else {
            logger.debug(
                "Dispositivo {} não possui lastActivityTime ou é 0, considerando inativo.",
                rs.getString("device_id")
            )
        }

        DeviceSummaryDTO(
            id = UUID.fromString(rs.getString("device_id")),
            name = rs.getString("device_name"),
            label = rs.getString("device_label"),
            type = rs.getString("device_type"),
            isActive = isActive
        )
    }

    fun getDeviceSummariesForUser(userIdStr: String): List<DeviceSummaryDTO> {
        val userUuid: UUID = UUID.fromString(userIdStr)
        val userInfoSql = "SELECT tenant_id, customer_id FROM tb_user WHERE id = ?"
        val userInfo: UserInfo? = try {
            jdbcTemplate.queryForObject(userInfoSql, userInfoRowMapper, userUuid)
        } catch (e: Exception) {
            logger.warn("Usuário não encontrado com ID: {}", userUuid)
            throw IllegalStateException("Informações do usuário não encontradas para ID: $userUuid")
        }

        if (userInfo == null) {
            logger.info("Informações do usuário não encontradas para ID: {}", userUuid)
            throw IllegalStateException("Informações do usuário não encontradas para ID: $userUuid")
        }

        var devicesSql = """
            SELECT 
                d.id AS device_id, 
                d.name AS device_name, 
                d.label AS device_label, 
                d.type AS device_type,
                attr_lat.long_v AS last_activity_ts 
            FROM 
                device d
            LEFT JOIN
                attribute_kv attr_lat ON d.id = attr_lat.entity_id
               AND attr_lat.attribute_type = '2' /*SERVER_SCOPE*/ 
               AND attr_lat.attribute_key = '86' /*lastActivityTime*/  
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
        return jdbcTemplate.query(devicesSql, deviceSummaryRowMapper, *queryParams.toTypedArray())
    }
}