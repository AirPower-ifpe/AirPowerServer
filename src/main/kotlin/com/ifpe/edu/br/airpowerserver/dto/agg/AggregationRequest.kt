package com.ifpe.edu.br.airpowerserver.dto.agg

import java.util.UUID

data class AggregationRequest(
    val devicesIds: List<UUID>,
    val aggKey: String,
    val aggStrategy: String,
    val timeInterval: String
)
{
    override fun toString(): String {
        return "AggregationRequest(devices=$devicesIds, aggKey='$aggKey', aggStrategy='$aggStrategy', timeInterval='$timeInterval')"
    }
}