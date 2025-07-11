package com.ifpe.edu.br.airpowerserver.dto.agg

data class ChartDataWrapper(
    val label: String,
    val chartData: List<ChatEntry>
) {
    override fun toString(): String {
        return "ChartDataWrapper(label='$label', chartData=$chartData)"
    }
}