package com.ifpe.edu.br.airpowerserver.dto.agg

data class AggQueryTsWrapper(
    val startTs: Long,
    val endTs: Long,
)
{
    override fun toString(): String {
        return "AggQueryTsWrapper(startTs=$startTs, endTs=$endTs)"
    }
}