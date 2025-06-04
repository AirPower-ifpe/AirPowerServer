package com.ifpe.edu.br.airpowerserver.dto

data class Telemetry(
    val key: String,
    val value: Double?,
    val dataPointsConsidered: Int
)
