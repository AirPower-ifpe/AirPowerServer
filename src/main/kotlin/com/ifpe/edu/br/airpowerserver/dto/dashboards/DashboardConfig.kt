package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class DashboardConfig(
    val configuration: DashboardConfiguration
)
{
    override fun toString(): String {
        return "DashboardConfig(configuration=$configuration)"
    }
}