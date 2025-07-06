package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.dto.alarms.APAlarmInfo
import com.ifpe.edu.br.airpowerserver.dto.alarms.TBAlarmResponse

fun TBAlarmResponse.toAirPowerAlarmInfo(): List<APAlarmInfo> {
    return data.map { alarm ->
        try {
            APAlarmInfo(
                id = alarm.id,
                createdTime = alarm.createdTime,
                tenantId = alarm.tenantId,
                customerId = alarm.customerId,
                type = alarm.type,
                originator = alarm.originator,
                severity = alarm.severity,
                acknowledged = alarm.acknowledged,
                cleared = alarm.cleared,
                assigneeId = alarm.assigneeId,
                originatorName = alarm.originatorName,
                originatorLabel = alarm.originatorLabel,
                assignee = alarm.assignee,
                name = alarm.name,
                status = alarm.status,
            )
        } catch (e: Exception) {
            throw IllegalStateException("Falha na transformação para List<APAlarmInfo> : ${e.message}")
        }
    }
}