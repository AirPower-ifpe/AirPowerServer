package com.ifpe.edu.br.airpowerserver.dto.dashboards

import com.ifpe.edu.br.airpowerserver.dto.Id

data class Datasource(
    val entityId: Id?,
    val deviceId: Id?
)
{
    override fun toString(): String {
        return "Datasource(entityId=$entityId, deviceId=$deviceId)"
    }
}