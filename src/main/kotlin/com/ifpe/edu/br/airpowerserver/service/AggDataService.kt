package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.agg.Agg
import com.ifpe.edu.br.airpowerserver.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.airpowerserver.dto.agg.AggregationRequest
import com.ifpe.edu.br.airpowerserver.dto.agg.ChartDataWrapper
import com.ifpe.edu.br.airpowerserver.dto.agg.ChatEntry
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class AggDataService(
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(AggDataService::class.java)


    fun getAggDataWrapper(
        request: AggregationRequest
    ): AggDataWrapperResponse{
        val devicesIds = request.devicesIds
        val aggKey = request.aggKey // VOLTAGE, POWER, CURRENT
        val aggStrategy = request.aggStrategy // AVG, SUM, MAX
        val timeInterval = request.timeInterval // DAY, WEEK, MONTH, YEAR

        // data wrapper label
        val aggLabel = "Consumo $timeInterval"

        // chard data entries and label
        val data = ChartDataWrapper(
            label = aggKey,
            listOf(
                ChatEntry(
                    "jan",
                    150
                ),
                ChatEntry(
                    "fev",
                    250
                ),
                ChatEntry(
                    "mar",
                    550
                ),
                ChatEntry(
                    "abr",
                    50
                ),
                ChatEntry(
                    "mai",
                    140
                ),
                ChatEntry(
                    "jun",
                    250
                ),
                ChatEntry(
                    "jul",
                    350
                ),
                ChatEntry(
                    "ago",
                    0
                ),
                ChatEntry(
                    "set",
                    0
                ),
                ChatEntry(
                    "out",
                    0
                ),
                ChatEntry(
                    "nov",
                    0
                ),
                ChatEntry(
                    "dez",
                    0
                )
            )
        )

        // aggregated value for given keys and strategy
        val agg = Agg(
            label = "Consumo no ano",
            value = "248,57"
        )

        return AggDataWrapperResponse(
            label = aggLabel,
            chartDataWrapper = data,
            aggregation = agg,
            size = 15)
    }
}