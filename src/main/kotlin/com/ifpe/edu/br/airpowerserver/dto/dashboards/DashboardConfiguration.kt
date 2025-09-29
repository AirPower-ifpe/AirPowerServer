package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class DashboardConfiguration(
    val widgets: Map<String, Widget>
)
{
    override fun toString(): String {
        return "DashboardConfiguration(widgets=$widgets)"
    }
}