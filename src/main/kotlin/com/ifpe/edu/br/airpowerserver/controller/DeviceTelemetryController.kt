package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationRequest
import com.ifpe.edu.br.airpowerserver.dto.TelemetryAggregationResponse
import com.ifpe.edu.br.airpowerserver.service.DeviceTelemetryService
import jakarta.validation.Valid
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
}