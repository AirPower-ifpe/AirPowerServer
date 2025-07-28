package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingsBoardActionButtonConfig(
    val enabled: Boolean
)
{
    override fun toString(): String {
        return "ThingsBoardActionButtonConfig(enabled=$enabled)"
    }
}