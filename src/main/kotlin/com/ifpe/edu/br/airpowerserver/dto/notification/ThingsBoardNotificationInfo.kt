package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ifpe.edu.br.airpowerserver.dto.Id

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingsBoardNotificationInfo(
    val type: String?,
    val alarmType: String?,
    val action: String?,
    val alarmId: String?,
    val alarmOriginator: Id?,
    val alarmOriginatorName: String?,
    val alarmSeverity: String?,
    val alarmStatus: String?,
    val acknowledged: Boolean,
    val cleared: Boolean,
    val alarmCustomerId: Id?,
    val dashboardId: Id?,
    val stateEntityId: Id?,
    val affectedCustomerId: Id?,
    val affectedUserId: Id?
)
{
    override fun toString(): String {
        return "ThingsBoardNotificationInfo(type='$type'," +
                " alarmType='$alarmType'," +
                " action='$action', " +
                "alarmId='$alarmId', " +
                "alarmOriginator=$alarmOriginator, " +
                "alarmOriginatorName='$alarmOriginatorName', " +
                "alarmSeverity='$alarmSeverity', " +
                "alarmStatus='$alarmStatus', " +
                "acknowledged=$acknowledged," +
                " cleared=$cleared, " +
                "alarmCustomerId=$alarmCustomerId, " +
                "dashboardId=$dashboardId, " +
                "stateEntityId=$stateEntityId, " +
                "affectedCustomerId=$affectedCustomerId, " +
                "affectedUserId=$affectedUserId)"
    }
}