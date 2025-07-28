package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingsBoardIconConfig(
    val enabled: Boolean,
    val icon: String,
    val color: String
)
{
    override fun toString(): String {
        return "ThingsBoardIconConfig(enabled=$enabled, icon='$icon', color='$color')"
    }
}