package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ifpe.edu.br.airpowerserver.dto.Id

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingsBoardNotification(
    val requestId: Id,
    val recipientId: Id,
    val type: String,
    val deliveryMethod: String,
    val subject: String,
    val text: String,
    val additionalConfig: ThingsBoardAdditionalConfig,
    val info: ThingsBoardNotificationInfo,
    val status: String,
    val id: Id,
    val createdTime: Long
)
{
    override fun toString(): String {
        return "ThingsBoardNotification(requestId=$requestId, recipientId=$recipientId, type='$type', deliveryMethod='$deliveryMethod', subject='$subject', text='$text', additionalConfig=$additionalConfig, info=$info, status='$status', id=$id, createdTime=$createdTime)"
    }
}