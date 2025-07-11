package com.ifpe.edu.br.airpowerserver.dto.agg

data class AggDataWrapperResponse(
    val label: String,
    val chartDataWrapper: ChartDataWrapper,
    val aggregation: Agg,
    val size: Int
)
{
    override fun toString(): String {
        return "AggDataWrapper(" +
                "label='$label', " +
                "chartDataWrapper=$chartDataWrapper, " +
                "aggregation=$aggregation, " +
                "size=$size" +
                ")"
    }
}