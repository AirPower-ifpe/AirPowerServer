package com.ifpe.edu.br.airpowerserver.dto.agg

data class ChatEntry(
    val label: String,
    val value: Long
)
{
    override fun toString(): String {
        return "ChatEntry(label='$label', timeWindowHours=$value)"
    }
}