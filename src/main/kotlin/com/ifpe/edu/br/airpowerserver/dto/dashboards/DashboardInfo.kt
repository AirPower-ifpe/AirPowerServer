package com.ifpe.edu.br.airpowerserver.dto.dashboards

import com.ifpe.edu.br.airpowerserver.dto.Id

data class DashboardInfo(
    val id: Id,
    val name: String,
    val title: String,
    var devicesIds: List<String> = emptyList()
)
{
    override fun toString(): String {
        return "DashboardInfo(id=$id, name='$name', title='$title', devicesIds=$devicesIds)"
    }
}