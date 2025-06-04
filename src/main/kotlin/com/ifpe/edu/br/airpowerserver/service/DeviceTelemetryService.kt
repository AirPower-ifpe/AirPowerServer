package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.DeviceAggregatedTelemetry
import com.ifpe.edu.br.airpowerserver.dto.Telemetry
import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class DeviceTelemetryService(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(DeviceTelemetryService::class.java)

    private val deviceLabelRowMapper = RowMapper<String?> { rs: ResultSet, _: Int ->
        rs.getString("label")
    }

    private fun getTelemetryKeyIdFromString(telemetryKeyString: String): Int? {
        val query = "SELECT key_id FROM key_dictionary WHERE key = ?"
        return try {
            jdbcTemplate.queryForObject(query, Int::class.java, telemetryKeyString)
        } catch (e: EmptyResultDataAccessException) {
            logger.warn(
                "Telemetry key not found for string: '{}' on dictionary: (ts_kv_dictionary).",
                "keyString: $telemetryKeyString  message:${e.message}"
            )
            null
        } catch (e: Exception) {
            logger.error("Error while find key_id for key string '{}': {}", telemetryKeyString, e.message)
            null
        }
    }

    /**
     * This function is not in a 'state of the art', it's just a first implementation to solve a problem,
     * need to be enhanced as soon as possible to improve performance on a huge bunch of devices
     */
    fun aggregateTelemetry(
        request: TelemetryAggregationRequest
    ): List<DeviceAggregatedTelemetry> {

        val results = mutableListOf<DeviceAggregatedTelemetry>()
        val endTime = Instant.now()
        val startTime = endTime.minus(request.timeWindowHours.toLong(), ChronoUnit.HOURS)
        val startTs = startTime.toEpochMilli()
        val endTs = endTime.toEpochMilli()

        for (deviceIdString in request.deviceIds) {
            val deviceUuid: UUID
            val aggregatedTelemetries = mutableListOf<Telemetry>()
            try {
                deviceUuid = UUID.fromString(deviceIdString)
            } catch (e: Exception) {
                logger.warn("Invalid ID for device: {}", "ID: $deviceIdString Message:${e.message}")
                continue
            }
            var deviceLabel: String?
            try {
                val query = "SELECT label FROM device WHERE id = ?"
                deviceLabel =
                    jdbcTemplate.query(query, deviceLabelRowMapper, deviceUuid)
                        .firstOrNull() ?: "Device not found"
            } catch (e: Exception) {
                logger.error("Error while searching for device: {}: {}", deviceUuid, e.message)
                deviceLabel = "DEVICE NOT_FOUND: $deviceUuid"
            }

            val telemetryQuery = """
                SELECT dbl_v, long_v 
                FROM ts_kv 
                WHERE entity_id = ? 
                  AND key = ? 
                  AND ts >= ? 
                  AND ts < ?
                  AND (dbl_v IS NOT NULL OR long_v IS NOT NULL)
            """.trimIndent()

            for (telemetryKey in request.telemetryKeys) {
                val keyId = getTelemetryKeyIdFromString(telemetryKey)
                if (keyId == null) {
                    logger.warn("Telemetry key ID not found '{}'", telemetryKey)
                    continue
                }
                try {
                    val values = jdbcTemplate.query(
                        telemetryQuery,
                        { rs: ResultSet, _: Int ->
                            val dblVal = rs.getObject("dbl_v") as? Number
                            val longVal = rs.getObject("long_v") as? Number
                            (dblVal?.toDouble() ?: longVal?.toDouble())
                        },
                        deviceUuid, keyId, startTs, endTs
                    ).filterNotNull()
                    val dataPointsCount = values.size
                    val aggregatedValue: Double? = if (values.isNotEmpty()) {
                        when (request.aggregationFunction.uppercase()) {
                            "AVG" -> values.average()
                            "SUM" -> values.sum()
                            "MIN" -> values.minOrNull()
                            "MAX" -> values.maxOrNull()
                            "COUNT" -> values.size.toDouble()
                            else -> {
                                logger.warn("Aggregation function not found: {}", request.aggregationFunction)
                                null
                            }
                        }
                    } else {
                        null
                    }
                    aggregatedTelemetries.add(
                        Telemetry(
                            key = telemetryKey,
                            value = aggregatedValue,
                            dataPointsConsidered = dataPointsCount
                        )
                    )
                } catch (e: Exception) {
                    logger.error(
                        "Error while aggregate telemetry for device:{}: {}",
                        deviceUuid,
                        e.message
                    )
                }
            }

            results.add(
                DeviceAggregatedTelemetry(
                    deviceId = deviceUuid,
                    deviceLabel = deviceLabel,
                    telemetryKeys = request.telemetryKeys,
                    aggregationFunction = request.aggregationFunction,
                    timeWindowHours = request.timeWindowHours,
                    aggregatedValues = aggregatedTelemetries,
                )
            )
        }
        logger.warn("ATTENTION, THIS FUNCTION NEEDS TO BE ENHANCED DUE TO REACH A BETTER PERFORMANCE AS SOON AS POSSIBLE")
        return results
    }
}