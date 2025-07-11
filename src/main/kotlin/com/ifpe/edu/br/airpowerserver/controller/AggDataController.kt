package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.agg.AggDataWrapperResponse
import com.ifpe.edu.br.airpowerserver.dto.agg.AggregationRequest
import com.ifpe.edu.br.airpowerserver.service.AggDataService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/agg-data/")
@RestController
class AggDataController(
    val aggDataService: AggDataService
) {

    private val logger = LoggerFactory.getLogger(AggDataController::class.java)

    @PostMapping("telemetry")
    fun getDeviceAggregatedDataWrapper(
        @Valid @RequestBody request: AggregationRequest
    ): ResponseEntity<AggDataWrapperResponse> {
        logger.info("Received getDeviceAggregatedDataWrapper request: $request")
        return ResponseEntity.ok(aggDataService.getAggDataWrapper(request))
    }
}