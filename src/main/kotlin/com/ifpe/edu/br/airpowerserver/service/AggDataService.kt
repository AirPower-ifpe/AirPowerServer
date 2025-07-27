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

    /**
     * Orquestra a busca e agregação de dados de telemetria e status.
     *
     * Este é o método principal do serviço. Ele:
     * 1. Busca os dados agregados esparsos do banco de dados.
     * 2. Gera uma série temporal completa para o período solicitado.
     * 3. Preenche os intervalos sem dados com o valor zero.
     * 4. Busca o valor total agregado e o status dos dispositivos.
     * 5. Monta e retorna o objeto de resposta completo.
     *
     * @param request O objeto [AggregationRequest] com os detalhes da solicitação.
     * @return Um [AggDataWrapperResponse] populado com todos os dados consolidados.
     */
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

    /**
     * Gera uma lista completa de todos os labels de tempo (ex: meses, dias)
     * para um determinado intervalo.
     */
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

    /**
     * Preenche uma lista esparsa de entradas de gráfico para garantir que todos os intervalos de tempo
     * dentro do período estejam presentes, adicionando valor 0 para os ausentes.
     *
     * @param sparseEntries A lista de dados retornada diretamente do banco de dados.
     * @param tsWrapper O wrapper que contém o intervalo de tempo completo e a formatação.
     * @return Uma lista de [ChartEntry] completa e ordenada.
     */
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

    /**
     * Busca os dados agregados do banco de dados. Retorna uma lista esparsa,
     * contendo apenas os intervalos que possuem dados.
     */
    private fun getSparseChartEntries(
        request: AggregationRequest,
        tsWrapper: AggQueryTsWrapper,
        deviceUuids: List<UUID>
    ): List<ChartEntry> {
        val params = MapSqlParameterSource()
            .addValue("deviceIds", deviceUuids)
            .addValue("aggKey", request.aggKey.name.lowercase())
            .addValue("startTs", tsWrapper.startTs)
            .addValue("endTs", tsWrapper.endTs)

        val safeAggStrategy = request.aggStrategy.name
        val chartSql = """
            SELECT
                DATE_TRUNC(:timeGroup, to_timestamp(t.ts / 1000)) AS time_bucket,
                ${safeAggStrategy}(t.long_v) AS aggregated_value
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
        return namedJdbcTemplate.query(finalChartSql, params, RowMapper { rs: ResultSet, _: Int ->
            ChartEntry(
                label = tsWrapper.timeFormat(rs.getTimestamp("time_bucket").time),
                value = rs.getLong("aggregated_value")
            )
        })
    }

    /**
     * Busca e calcula o resumo de status (Ativos/Inativos) para uma lista de dispositivos.
     * A busca é feita consultando o atributo de servidor 'active' na tabela 'attribute_kv'.
     *
     * @param deviceIds Lista de UUIDs dos dispositivos a serem verificados.
     * @return Uma lista de [DevicesStatusSummary].
     */
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
            .addValue("aggKey", request.aggKey.name.lowercase())
            .addValue("startTs", tsWrapper.startTs)
            .addValue("endTs", tsWrapper.endTs)

        val safeAggStrategy = request.aggStrategy.name
        val totalAggSql = """
            SELECT $safeAggStrategy(t.long_v)
            FROM ts_kv AS t
            JOIN key_dictionary AS d ON t.key = d.key_id
            WHERE t.entity_id IN (:deviceIds) AND d.key = :aggKey AND t.ts BETWEEN :startTs AND :endTs
        """.trimIndent()

        return namedJdbcTemplate.queryForObject(totalAggSql, params, Long::class.java) ?: 0L
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
}