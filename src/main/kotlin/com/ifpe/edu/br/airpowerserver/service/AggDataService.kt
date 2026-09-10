package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.DeviceAggregatedTelemetry
import com.ifpe.edu.br.airpowerserver.dto.Telemetry
import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationRequest
import com.ifpe.edu.br.airpowerserver.dto.agg.*
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * Serviço responsável por realizar a agregação de dados de telemetria e status de dispositivos.
 *
 * Este serviço se comunica diretamente com o banco de dados do ThingsBoard para
 * executar consultas SQL otimizadas, buscando dados agregados de forma performática.
 *
 * @property namedJdbcTemplate O template para execução de queries SQL com parâmetros nomeados.
 */
@Service
class AggDataService(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(AggDataService::class.java)
    private val ptBrLocale = Locale("pt", "BR")

    fun getAggDataWrapper(
        request: AggregationRequest
    ): AggDataWrapperResponse {
        logger.info("getAggDataWrapper(): request: $request")
        val tsWrapper = parseTimeInterval(request.timeIntervalWrapper)
        val deviceUuids = request.devicesIds.map { UUID.fromString(it) }

        val sparseChartEntries = getSparseChartEntries(request, tsWrapper, deviceUuids)
        val completeChartEntries = padChartEntries(sparseChartEntries, tsWrapper)

        val totalValue = getTotalAggregatedValue(request, tsWrapper, deviceUuids)
        val statusSummaries = getDevicesStatusSummary(deviceUuids)

        logger.info("getAggDataWrapper(): Concluido. Entradas de grafico: ${completeChartEntries.size}, Total calculado: $totalValue")

        return AggDataWrapperResponse(
            label = "Consumo ${parseWrapperLabel(request.timeIntervalWrapper.timeInterval)}",
            chartDataWrapper = ChartDataWrapper(
                label = parseAggKey(request.aggKey),
                entries = completeChartEntries
            ),
            statusSummaries = statusSummaries,
            aggregation = Agg(
                label = "Total no período",
                value = "$totalValue"
            ),
            size = request.devicesIds.size
        )
    }

    private fun generateCompleteTimeLabels(tsWrapper: AggQueryTsWrapper): List<String> {
        val labels = mutableListOf<String>()
        var currentTs = ZonedDateTime.ofInstant(Instant.ofEpochMilli(tsWrapper.startTs), ZoneId.systemDefault())
        val endTs = ZonedDateTime.ofInstant(Instant.ofEpochMilli(tsWrapper.endTs), ZoneId.systemDefault())

        while (currentTs.isBefore(endTs)) {
            labels.add(tsWrapper.timeFormat(currentTs.toInstant().toEpochMilli()))
            currentTs = when (tsWrapper.timeGroup) {
                "hour" -> currentTs.plusHours(1)
                "day" -> currentTs.plusDays(1)
                "month" -> currentTs.plusMonths(1)
                else -> break
            }
        }
        return labels
    }

    private fun padChartEntries(sparseEntries: List<ChartEntry>, tsWrapper: AggQueryTsWrapper): List<ChartEntry> {
        val dataMap = sparseEntries.associateBy { it.label }
        val allPossibleLabels = generateCompleteTimeLabels(tsWrapper)
        return allPossibleLabels.map { label ->
            ChartEntry(
                label = label,
                value = dataMap[label]?.value ?: 0L
            )
        }
    }

    private fun getSparseChartEntries(
        request: AggregationRequest,
        tsWrapper: AggQueryTsWrapper,
        deviceUuids: List<UUID>
    ): List<ChartEntry> {
        val params = MapSqlParameterSource()
            .addValue("deviceIds", deviceUuids)
            .addValue("aggKey", request.aggKey.name.lowercase().trim())
            .addValue("startTs", tsWrapper.startTs)
            .addValue("endTs", tsWrapper.endTs)

        val safeAggStrategy = request.aggStrategy.name
        val chartSql = """
            SELECT
                DATE_TRUNC(:timeGroup, to_timestamp(t.ts / 1000)) AS time_bucket,
                ROUND(${safeAggStrategy}(COALESCE(t.dbl_v, t.long_v::double precision)))::bigint AS aggregated_value
            FROM ts_kv AS t
            JOIN key_dictionary AS d ON t.key = d.key_id
            WHERE
                t.entity_id IN (:deviceIds) AND
                d.key = :aggKey AND
                t.ts BETWEEN :startTs AND :endTs
            GROUP BY time_bucket
            ORDER BY time_bucket
        """.trimIndent()

        val finalChartSql = chartSql.replace(":timeGroup", "'${tsWrapper.timeGroup}'")
        val entries = namedJdbcTemplate.query(finalChartSql, params, RowMapper { rs: ResultSet, _: Int ->
            ChartEntry(
                label = tsWrapper.timeFormat(rs.getTimestamp("time_bucket").time),
                value = rs.getLong("aggregated_value")
            )
        })

        logger.info("getSparseChartEntries(): Encontradas ${entries.size} faixas temporais com dados para a chave '${request.aggKey.name.lowercase()}'")
        return entries
    }

    private fun getDevicesStatusSummary(deviceIds: List<UUID>): List<DevicesStatusSummary> {
        if (deviceIds.isEmpty()) return emptyList()
        val sql = """
            SELECT
                bool_v AS is_active,
                COUNT(*) AS status_count
            FROM attribute_kv
            WHERE
                entity_id IN (:deviceIds) AND
                attribute_key = (SELECT key_id FROM key_dictionary WHERE key = 'active')
            GROUP BY
                is_active;
        """.trimIndent()

        val params = MapSqlParameterSource("deviceIds", deviceIds)

        val results = namedJdbcTemplate.query(sql, params) { rs: ResultSet, _: Int ->
            rs.getBoolean("is_active") to rs.getInt("status_count")
        }.toMap()

        val activeCount = results[true] ?: 0
        val inactiveCount = results[false] ?: 0

        return listOf(
            DevicesStatusSummary("Ativos", activeCount),
            DevicesStatusSummary("Inativos", inactiveCount)
        )
    }

    private fun getTotalAggregatedValue(
        request: AggregationRequest,
        tsWrapper: AggQueryTsWrapper,
        deviceUuids: List<UUID>
    ): Long {
        val params = MapSqlParameterSource()
            .addValue("deviceIds", deviceUuids)
            .addValue("aggKey", request.aggKey.name.lowercase().trim())
            .addValue("startTs", tsWrapper.startTs)
            .addValue("endTs", tsWrapper.endTs)

        val safeAggStrategy = request.aggStrategy.name
        val totalAggSql = """
            SELECT ROUND($safeAggStrategy(COALESCE(t.dbl_v, t.long_v::double precision)))::bigint
            FROM ts_kv AS t
            JOIN key_dictionary AS d ON t.key = d.key_id
            WHERE t.entity_id IN (:deviceIds) AND d.key = :aggKey AND t.ts BETWEEN :startTs AND :endTs
        """.trimIndent()

        return try {
            val total = namedJdbcTemplate.queryForObject(totalAggSql, params, Long::class.java) ?: 0L
            logger.info("getTotalAggregatedValue(): Total no periodo = $total")
            total
        } catch (e: Exception) {
            logger.warn("getTotalAggregatedValue(): Nenhum dado agregado encontrado ou falha na consulta (${e.message}). Retornando 0.")
            0L
        }
    }

    private fun parseAggKey(aggKey: TelemetryKey): String {
        return when (aggKey) {
            TelemetryKey.POWER -> "KW/h"
            TelemetryKey.VOLTAGE -> "Volts"
            TelemetryKey.CURRENT -> "Amperes"
        }
    }

    private fun parseWrapperLabel(interval: TimeInterval): String {
        return when (interval) {
            TimeInterval.DAY -> "diário"
            TimeInterval.WEEK -> "semanal"
            TimeInterval.MONTH -> "mensal"
            TimeInterval.YEAR -> "anual"
        }
    }

    private fun parseTimeInterval(
        timeIntervalWrapper: TimeIntervalWrapper,
    ): AggQueryTsWrapper {
        val rawStart = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(timeIntervalWrapper.periodStartTs),
            ZoneId.systemDefault()
        ).withNano(0)

        val now = ZonedDateTime.now(rawStart.zone)

        return when (timeIntervalWrapper.timeInterval) {
            TimeInterval.DAY -> {
                val start = rawStart.truncatedTo(ChronoUnit.DAYS)
                val end = if (start.isBefore(now.truncatedTo(ChronoUnit.DAYS))) start.plusDays(1) else now
                AggQueryTsWrapper(
                    startTs = start.toInstant().toEpochMilli(),
                    endTs = end.toInstant().toEpochMilli(),
                    timeGroup = "hour",
                    timeFormat = { ts ->
                        Instant.ofEpochMilli(ts).atZone(start.zone).hour.toString()
                    }
                )
            }

            TimeInterval.WEEK -> {
                val start =
                    rawStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS)
                val end = if (start.isBefore(
                        now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS)
                    )
                ) start.plusWeeks(1) else now
                AggQueryTsWrapper(
                    startTs = start.toInstant().toEpochMilli(),
                    endTs = end.toInstant().toEpochMilli(),
                    timeGroup = "day",
                    timeFormat = { ts ->
                        Instant.ofEpochMilli(ts).atZone(start.zone)
                            .dayOfWeek.getDisplayName(TextStyle.SHORT, ptBrLocale)
                    }
                )
            }

            TimeInterval.MONTH -> {
                val start = rawStart.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
                val end = if (start.isBefore(
                        now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS)
                    )
                ) start.plusMonths(1) else now
                AggQueryTsWrapper(
                    startTs = start.toInstant().toEpochMilli(),
                    endTs = end.toInstant().toEpochMilli(),
                    timeGroup = "day",
                    timeFormat = { ts ->
                        Instant.ofEpochMilli(ts).atZone(start.zone).dayOfMonth.toString()
                    }
                )
            }

            TimeInterval.YEAR -> {
                val start = rawStart.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS)
                val end = if (start.isBefore(
                        now.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS)
                    )
                ) start.plusYears(1) else now
                AggQueryTsWrapper(
                    startTs = start.toInstant().toEpochMilli(),
                    endTs = end.toInstant().toEpochMilli(),
                    timeGroup = "month",
                    timeFormat = { ts ->
                        Instant.ofEpochMilli(ts).atZone(start.zone)
                            .month.getDisplayName(TextStyle.SHORT, ptBrLocale)
                    }
                )
            }
        }
    }

    private val deviceLabelRowMapper = RowMapper<String?> { rs: ResultSet, _: Int ->
        rs.getString("label")
    }

    fun aggregateTelemetry(
        request: TelemetryAggregationRequest
    ): List<DeviceAggregatedTelemetry> {
        val results = mutableListOf<DeviceAggregatedTelemetry>()
        val endTime = Instant.now()
        val startTime = endTime.minus(request.timeWindowHours.toLong(), ChronoUnit.HOURS)
        val startTs = startTime.toEpochMilli()
        val endTs = endTime.toEpochMilli()

        logger.info("aggregateTelemetry(): Processando janela de ${request.timeWindowHours}h (entre $startTs e $endTs) para ${request.deviceIds.size} dispositivo(s)")

        for (deviceIdString in request.deviceIds) {
            val deviceUuid: UUID
            val aggregatedTelemetries = mutableListOf<Telemetry>()
            try {
                deviceUuid = UUID.fromString(deviceIdString)
            } catch (e: Exception) {
                logger.warn("aggregateTelemetry(): ID de dispositivo invalido: $deviceIdString")
                continue
            }
            var deviceLabel: String?
            try {
                val query = "SELECT label FROM device WHERE id = ?"
                deviceLabel =
                    jdbcTemplate.query(query, deviceLabelRowMapper, deviceUuid)
                        .firstOrNull() ?: "Device not found"
            } catch (e: Exception) {
                logger.error("aggregateTelemetry(): Erro ao buscar label do device $deviceUuid: ${e.message}")
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
                    logger.warn("aggregateTelemetry(): ID da chave nao encontrado para '$telemetryKey'")
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
                                logger.warn("aggregateTelemetry(): Funcao de agregacao desconhecida: ${request.aggregationFunction}")
                                null
                            }
                        }
                    } else {
                        null
                    }

                    logger.info("aggregateTelemetry(): Device $deviceUuid, Chave '$telemetryKey', Pontos lidos: $dataPointsCount, Valor agregado: $aggregatedValue")

                    aggregatedTelemetries.add(
                        Telemetry(
                            key = telemetryKey,
                            value = aggregatedValue,
                            dataPointsConsidered = dataPointsCount
                        )
                    )
                } catch (e: Exception) {
                    logger.error("aggregateTelemetry(): Erro ao agregar telemetria para device $deviceUuid: ${e.message}")
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
        return results
    }

    private fun getTelemetryKeyIdFromString(telemetryKeyString: String): Int? {
        val normalizedKey = telemetryKeyString.trim().lowercase()
        val query = "SELECT key_id FROM key_dictionary WHERE key = ?"
        return try {
            jdbcTemplate.queryForObject(query, Int::class.java, normalizedKey)
        } catch (e: EmptyResultDataAccessException) {
            logger.warn("getTelemetryKeyIdFromString(): Chave nao encontrada no dicionario: '$normalizedKey'")
            null
        } catch (e: Exception) {
            logger.error("getTelemetryKeyIdFromString(): Erro ao consultar chave '$normalizedKey': ${e.message}")
            null
        }
    }
}