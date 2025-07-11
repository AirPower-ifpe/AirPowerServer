package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.agg.*
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * Serviço responsável por realizar a agregação de dados de telemetria.
 *
 * Este serviço se comunica diretamente com o banco de dados do ThingsBoard para
 * executar consultas SQL otimizadas, buscando dados agregados de forma performática.
 * Ele é projetado para suportar diferentes estratégias de agregação (AVG, SUM, etc.)
 * e diversos intervalos de tempo.
 *
 * @property jdbcTemplate O template para execução de queries SQL com parâmetros nomeados.
 */
@Service
class AggDataService(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(AggDataService::class.java)
    private val ptBrLocale = Locale("pt", "BR")

    /**
     * Orquestra a busca e agregação de dados de telemetria.
     *
     * Este é o método principal do serviço. Ele:
     * 1. Interpreta a requisição para definir o período de tempo.
     * 2. Constrói e executa duas queries SQL nativas:
     * - Uma para obter os dados agrupados para o gráfico.
     * - Outra para obter o valor total agregado no período.
     * 3. Mapeia os resultados e monta o objeto de resposta [AggDataWrapperResponse].
     *
     * @param request O objeto [AggregationRequest] com os detalhes da solicitação.
     * @return Um [AggDataWrapperResponse] populado com os dados reais do banco.
     */
    fun getAggDataWrapper(
        request: AggregationRequest
    ): AggDataWrapperResponse {
        logger.info("getAggDataWrapper(): request: $request")
        val tsWrapper = parseTimeInterval(request.timeIntervalWrapper)
        logger.info(
            "Calculated time range-> timeGroup:${tsWrapper.timeGroup} " +
                    "startTs=${tsWrapper.startTs} : ${formatEpochToDate(tsWrapper.startTs)}, " +
                    "endTs=${tsWrapper.endTs} : ${formatEpochToDate(tsWrapper.endTs)}"
        )

        val params = MapSqlParameterSource()
            .addValue("deviceIds", request.devicesIds.map { UUID.fromString(it) })
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
        val chartEntries = jdbcTemplate.query(finalChartSql, params, RowMapper { rs: ResultSet, _: Int ->
            ChartEntry(
                label = tsWrapper.timeFormat(rs.getTimestamp("time_bucket").time),
                value = rs.getLong("aggregated_value")
            )
        })

        val totalAggSql = """
            SELECT $safeAggStrategy(t.long_v)
            FROM ts_kv AS t
            JOIN key_dictionary AS d ON t.key = d.key_id
            WHERE t.entity_id IN (:deviceIds) AND d.key = :aggKey AND t.ts BETWEEN :startTs AND :endTs
        """.trimIndent()
        val totalValue = jdbcTemplate.queryForObject(totalAggSql, params, Long::class.java) ?: 0L

        return AggDataWrapperResponse(
            label = "Consumo ${parseWrapperLabel(request.timeIntervalWrapper.timeInterval)}",
            chartDataWrapper = ChartDataWrapper(
                label = parseAggKey(request.aggKey),
                entries = chartEntries
            ),
            aggregation = Agg(
                label = "Total no período",
                value = "$totalValue"
            ),
            size = request.devicesIds.size
        )
    }

    private fun formatEpochToDate(epochMillis: Long): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBrLocale)
            .withZone(ZoneId.of("America/Sao_Paulo"))
        return formatter.format(Instant.ofEpochMilli(epochMillis))
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

        return when (timeIntervalWrapper.timeInterval) {
            TimeInterval.DAY -> {
                val start = rawStart.truncatedTo(ChronoUnit.DAYS)
                val end = start.plusDays(1)
                AggQueryTsWrapper(
                    startTs = start.toInstant().toEpochMilli(),
                    endTs = end.toInstant().toEpochMilli(),
                    timeGroup = "hour",
                    timeFormat = { ts ->
                        Instant.ofEpochMilli(ts).atZone(start.zone).hour.toString() + "h"
                    }
                )
            }

            TimeInterval.WEEK -> {
                val start =
                    rawStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS)
                val end = start.plusWeeks(1)
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
                val end = start.plusMonths(1)
                AggQueryTsWrapper(
                    startTs = start.toInstant().toEpochMilli(),
                    endTs = end.toInstant().toEpochMilli(),
                    timeGroup = "day",
                    timeFormat = { ts ->
                        "Dia " + Instant.ofEpochMilli(ts).atZone(start.zone).dayOfMonth
                    }
                )
            }

            TimeInterval.YEAR -> {
                val start = rawStart.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS)
                val end = start.plusYears(1)
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
}