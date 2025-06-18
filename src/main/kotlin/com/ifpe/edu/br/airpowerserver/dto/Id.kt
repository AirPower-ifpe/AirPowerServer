package com.ifpe.edu.br.airpowerserver.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Id(
    val id: UUID,
    val entityType: String
) {
    override fun toString(): String {
        return "Id(id=$id, entityType='$entityType')"
    }
}