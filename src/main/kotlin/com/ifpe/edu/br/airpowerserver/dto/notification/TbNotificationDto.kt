package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TbNotificationDto(
    val createdTime: Long,
    val subject: String,
    val text: String,
    val status: String
)
{
    override fun toString(): String {
        return "TbNotificationDto(createdTime=$createdTime, subject='$subject', text='$text', status='$status')"
    }
}