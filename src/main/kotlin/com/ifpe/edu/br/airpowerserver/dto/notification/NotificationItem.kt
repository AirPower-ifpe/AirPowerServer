package com.ifpe.edu.br.airpowerserver.dto.notification

data class NotificationItem(
    val label: String,
    val message: String,
    val timestamp: Long,
    var isNew: Boolean = true
)
{
    override fun toString(): String {
        return "NotificationItem(label='$label', message='$message', timestamp=$timestamp, isNew=$isNew)"
    }
}