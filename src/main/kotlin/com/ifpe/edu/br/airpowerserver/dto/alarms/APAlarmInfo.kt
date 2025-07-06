package com.ifpe.edu.br.airpowerserver.dto.alarms

import java.util.UUID

data class APAlarmInfo(
    val id: UUID,
    val type: String,
    val message: String,
    val timestamp: Long,
    val occurrence: Int
)
{
    override fun toString(): String {
        return "APAlarmInfo(id=$id, type='$type', message='$message', timestamp=$timestamp, occurrence=$occurrence)"
    }
}