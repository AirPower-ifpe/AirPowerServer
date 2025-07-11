package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationRequest
import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationResponse
import com.ifpe.edu.br.airpowerserver.dto.agg.Agg
import com.ifpe.edu.br.airpowerserver.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.airpowerserver.dto.agg.AggregationRequest
import com.ifpe.edu.br.airpowerserver.dto.agg.ChartDataWrapper
import com.ifpe.edu.br.airpowerserver.dto.agg.ChatEntry
import com.ifpe.edu.br.airpowerserver.service.DeviceTelemetryService
import jakarta.validation.Valid
import lombok.`val`
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test/api/v1/devices")
class DeviceTelemetryController(private val deviceTelemetryService: DeviceTelemetryService) {

    private val logger = LoggerFactory.getLogger(DeviceTelemetryService::class.java)

    @PostMapping("/telemetry/aggregate")
    fun getAggregatedTelemetry(
        @Valid @RequestBody request: TelemetryAggregationRequest
    ): ResponseEntity<TelemetryAggregationResponse> {
        logger.warn("Received telemetry aggregation request: $request")
        val results = deviceTelemetryService.aggregateTelemetry(request)
        return ResponseEntity.ok(TelemetryAggregationResponse(results = results))
    }

    @PostMapping("/telemetry/aggregate/data-wrapper")
    fun getDeviceAggregatedDataWrapper(
        @Valid @RequestBody request: AggregationRequest
    ): ResponseEntity<AggDataWrapperResponse> {
        logger.info("Received getDeviceAggregatedDataWrapper request: $request")

        // data wrapper label
        val aggLabel = "Consumo anual"

        // chard data entries and label
        val data = ChartDataWrapper(
            label = "KW/h",
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

        // response wrapper
        return ResponseEntity.ok(
            AggDataWrapperResponse(
                label = aggLabel,
                chartDataWrapper = data,
                aggregation = agg,
                size = 15
            )
        )
    }
}