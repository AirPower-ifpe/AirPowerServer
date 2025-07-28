package com.ifpe.edu.br.airpowerserver.dto.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ThingsBoardAdditionalConfig(
    val icon: ThingsBoardIconConfig,
    val actionButtonConfig: ThingsBoardActionButtonConfig
) {
    override fun toString(): String {
        return "ThingsBoardAdditionalConfig(icon=$icon, actionButtonConfig=$actionButtonConfig)"
    }
}