package com.ifpe.edu.br.airpowerserver.dto.agg

import java.util.*

data class AggregationRequest(
    val devicesIds: List<UUID>,
    val aggKey: String,
    val aggStrategy: String,
    val timeInterval: TimeInterval
) {
    override fun toString(): String {
        return "AggregationRequest(devices=$devicesIds, aggKey='$aggKey', aggStrategy='$aggStrategy', timeInterval='$timeInterval')"
    }
}