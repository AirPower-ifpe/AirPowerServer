package com.ifpe.edu.br.airpowerserver.dto

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errorCode: Int,
    val timestamp: String
) {
    override fun toString(): String {
        return "ErrorResponse(status=$status, message='$message', errorCode=$errorCode, timestamp='$timestamp')"
    }
}