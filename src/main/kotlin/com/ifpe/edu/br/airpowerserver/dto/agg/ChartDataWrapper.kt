package com.ifpe.edu.br.airpowerserver.dto.agg

data class ChartDataWrapper(
    val label: String,
    val entries: List<ChartEntry>
) {
    override fun toString(): String {
        return "ChartDataWrapper(label='$label', entries=$entries)"
    }
}