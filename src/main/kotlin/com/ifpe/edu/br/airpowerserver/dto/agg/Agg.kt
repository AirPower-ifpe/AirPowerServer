package com.ifpe.edu.br.airpowerserver.dto.agg

data class Agg(
    val label: String,
    val value: String
)
{
    override fun toString(): String {
        return "Agg(label='$label', values='$value')"
    }
}