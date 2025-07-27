package com.ifpe.edu.br.airpowerserver.dto.agg

data class AggDataWrapperResponse(
    val label: String,
    val chartDataWrapper: ChartDataWrapper,
    val statusSummaries: List<DevicesStatusSummary>,
    val aggregation: Agg,
    val size: Int
) {
    override fun toString(): String {
        return "AggDataWrapperResponse(label='$label', " +
                "chartDataWrapper=$chartDataWrapper, " +
                "statusSummaries=$statusSummaries, " +
                "aggregation=$aggregation, " +
                "size=$size)"
    }
}