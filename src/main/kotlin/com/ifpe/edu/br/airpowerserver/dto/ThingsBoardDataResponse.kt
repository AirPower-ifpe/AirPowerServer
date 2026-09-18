package com.ifpe.edu.br.airpowerserver.dto

data class ThingsBoardDataResponse<T>(
    val data: List<T>
)
{
    override fun toString(): String {
        return "ThingsBoardDataResponse(data=$data)"
    }
}