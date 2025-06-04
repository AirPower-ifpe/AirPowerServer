package com.ifpe.edu.br.airpowerserver.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class TelemetryAggregationRequest(
    @field:NotEmpty(message = "Device ID list can't be empty")
    val deviceIds: List<String>,

    @field:NotNull(message = "telemetry key can't be null")
    val telemetryKeys: List<String>,

    @field:NotEmpty(message = "aggregation key can't be empty")
    val aggregationFunction: String,

    @field:Positive(message = "timeWindow can't be negative")
    val timeWindowHours: Int,
)