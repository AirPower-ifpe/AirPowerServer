package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingsBoardNotificationResponse(
    val data: List<ThingsBoardNotification>
) {
    override fun toString(): String {
        return "ThingsBoardNotificationResponse(data=$data)"
    }
}