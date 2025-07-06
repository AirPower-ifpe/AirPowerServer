package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.dto.alarms.APAlarmInfo
import com.ifpe.edu.br.airpowerserver.dto.alarms.TBAlarmResponse

fun TBAlarmResponse.toAirPowerAlarmInfo(): List<APAlarmInfo> {
    return data.map { alarm ->
        try {
            APAlarmInfo(
                id = alarm.id.id,
                type = alarm.type,
                message = alarm.originatorName ?: "Sem origem",
                timestamp = alarm.startTs,
                occurrence = 1
            )
        } catch (e: Exception) {
            throw IllegalStateException("Falha na transformação para List<APAlarmInfo> : ${e.message}")
        }
    }
}