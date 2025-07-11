package com.ifpe.edu.br.airpowerserver.dto.agg

data class AggregationRequest(
    val devicesIds: List<String>,
    val aggKey: TelemetryKey,
    val aggStrategy: AggStrategy,
    val timeIntervalWrapper: TimeIntervalWrapper
) {
    override fun toString(): String {
        return "AggregationRequest(" +
                "devices=$devicesIds, " +
                "aggKey='$aggKey', " +
                "aggStrategy='$aggStrategy', " +
                "timeInterval='$timeIntervalWrapper')"
    }
}