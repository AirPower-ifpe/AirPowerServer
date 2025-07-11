package com.ifpe.edu.br.airpowerserver.dto.agg

data class AggQueryTsWrapper(
    val startTs: Long,
    val endTs: Long,
    val timeGroup: String,
    val timeFormat: (ts: Long) -> String
)
{
    override fun toString(): String {
        return "AggQueryTsWrapper(startTs=$startTs, endTs=$endTs, timeGroup='$timeGroup', timeFormat=$timeFormat)"
    }
}