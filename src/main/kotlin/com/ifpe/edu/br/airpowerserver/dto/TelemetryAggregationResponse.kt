package com.ifpe.edu.br.airpowerserver.dto

data class TelemetryAggregationResponse(
    val results: List<DeviceAggregatedTelemetry>
)
