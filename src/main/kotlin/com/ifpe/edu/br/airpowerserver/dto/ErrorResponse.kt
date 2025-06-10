package com.ifpe.edu.br.airpowerserver.dto

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errorCode: Int,
    val timestamp: String
)