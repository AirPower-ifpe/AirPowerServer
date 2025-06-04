package com.ifpe.edu.br.airpowerserver.dto

import java.util.UUID

data class DeviceAggregatedTelemetry(
    val deviceId: UUID? = null,
    val deviceLabel: String? = null,
    val telemetryKeys: List<String>? = null,
    val aggregationFunction: String? = null,
    val timeWindowHours: Int? = null,
    val aggregatedValues: List<Telemetry>? = null,
)
