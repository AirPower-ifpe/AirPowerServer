package com.ifpe.edu.br.airpowerserver.dto.agg

data class TimeIntervalWrapper(
    val periodStartTs: Long,
    val timeInterval: TimeInterval,
)
{
    override fun toString(): String {
        return "TimeIntervalWrapper(periodStartTs=$periodStartTs, timeInterval=$timeInterval)"
    }
}