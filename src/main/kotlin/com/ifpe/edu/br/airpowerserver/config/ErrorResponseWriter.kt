package com.ifpe.edu.br.airpowerserver.config


import com.fasterxml.jackson.databind.ObjectMapper
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorResponse

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class ErrorResponseWriter(
    private val objectMapper: ObjectMapper
) {
    fun writeErrorResponse(response: HttpServletResponse, errorCode: ErrorCode) {
        val apiErrorResponse = ErrorResponse(
            status = errorCode.httpStatus,
            errorCode = errorCode.errorCode,
            message = errorCode.defaultMessage,
            timestamp = System.currentTimeMillis()
        )

        response.status = errorCode.httpStatus
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        objectMapper.writeValue(response.writer, apiErrorResponse)
    }
}