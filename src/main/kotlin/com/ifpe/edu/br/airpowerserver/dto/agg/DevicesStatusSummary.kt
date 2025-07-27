package com.ifpe.edu.br.airpowerserver.dto.agg

data class DevicesStatusSummary(
    val label: String,
    val occurrence: Int
)
{
    override fun toString(): String {
        return "DevicesStatusSummary(label='$label', occurrence=$occurrence)"
    }
}