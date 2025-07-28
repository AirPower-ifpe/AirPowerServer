package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ifpe.edu.br.airpowerserver.dto.Id

@JsonIgnoreProperties(ignoreUnknown = true)
data class AirPowerNotificationItem(
    var id: Id,
    val subject: String,
    val type: String,
    val text: String,
    val createdTime: Long,
    val alarmType: String?,
    val alarmId: String?,
    val alarmOriginator: Id?,
    val alarmOriginatorName: String?,
    val alarmSeverity: String?,
    val alarmStatus: String?,
    var dashboardId: Id?,
    var status: String
) {
    override fun toString(): String {
        return "AirPowerNotificationItem(id=$id," +
                "subject='$subject'," +
                "type='$type'," +
                "text='$text'," +
                "createdTime=$createdTime," +
                "alarmType='$alarmType'," +
                "alarmId='$alarmId'," +
                "alarmOriginator=$alarmOriginator," +
                "alarmOriginatorName=$alarmOriginatorName," +
                "alarmSeverity='$alarmSeverity'," +
                "alarmStatus='$alarmStatus'," +
                "dashboardId=$dashboardId," +
                "status='$status')"
    }
}