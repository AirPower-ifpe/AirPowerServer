package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class WidgetConfig(
    val datasources: List<Datasource>
)
{
    override fun toString(): String {
        return "WidgetConfig(datasources=$datasources)"
    }
}