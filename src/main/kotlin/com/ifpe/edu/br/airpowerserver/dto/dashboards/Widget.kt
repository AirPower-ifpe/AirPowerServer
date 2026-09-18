package com.ifpe.edu.br.airpowerserver.dto.dashboards

data class Widget(
    val config: WidgetConfig
)
{
    override fun toString(): String {
        return "Widget(config=$config)"
    }
}