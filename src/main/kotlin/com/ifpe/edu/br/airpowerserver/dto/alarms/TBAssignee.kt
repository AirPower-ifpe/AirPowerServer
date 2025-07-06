package com.ifpe.edu.br.airpowerserver.dto.alarms

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.ifpe.edu.br.airpowerserver.dto.Id

@JsonIgnoreProperties(ignoreUnknown = true)
data class TBAssignee(
    val id: Id,
    val firstName: String?,
    val lastName: String?,
    val email: String?
)
{
    override fun toString(): String {
        return "Assignee(id=$id, firstName=$firstName, lastName=$lastName, email=$email)"
    }
}