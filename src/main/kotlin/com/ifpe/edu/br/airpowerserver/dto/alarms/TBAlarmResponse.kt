package com.ifpe.edu.br.airpowerserver.dto.alarms

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TBAlarmResponse(
    val data: List<TBAlarm>,
    val totalPages: Int,
    val totalElements: Long,
    val hasNext: Boolean
)
{
    override fun toString(): String {
        return "ThingsBoardAlarmResponse(data=$data, totalPages=$totalPages, totalElements=$totalElements, hasNext=$hasNext)"
    }
}