package com.ifpe.edu.br.airpowerserver.dto.error

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse(
    val status: Int,
    val errorCode: Int,
    val message: String,
    val timestamp: Long
)