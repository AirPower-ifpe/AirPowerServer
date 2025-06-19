package com.ifpe.edu.br.airpowerserver.dto.error

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalApiExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ErrorResponse> {
        if (ex is DownstreamServiceException) {
            logger.warn(
                "Erro de serviço downstream tratado. Código: {}, HttpStatus: {}, Causa: {}",
                ex.errorCode.errorCode, ex.errorCode.httpStatus, ex.downstreamResponseBody
            )
        } else {
            logger.warn("Erro de API tratado. Código: {}, Mensagem: {}", ex.errorCode.errorCode, ex.message)
        }

        val errorResponse = ErrorResponse(
            status = ex.errorCode.httpStatus,
            errorCode = ex.errorCode.errorCode,
            message = ex.errorCode.defaultMessage,
            timestamp = System.currentTimeMillis()
        )
        return ResponseEntity(errorResponse, org.springframework.http.HttpStatus.valueOf(ex.errorCode.httpStatus))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Erro interno não esperado e não tratado.", ex)
        val errorResponse = ErrorResponse(
            status = ErrorCode.UNKNOWN_INTERNAL_ERROR.httpStatus,
            errorCode = ErrorCode.UNKNOWN_INTERNAL_ERROR.errorCode,
            message = ErrorCode.UNKNOWN_INTERNAL_ERROR.defaultMessage,
            timestamp = System.currentTimeMillis()
        )
        return ResponseEntity(errorResponse, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        logger.error("Erro interno do servidor: ", ex)
        val errorResponse = ErrorResponse(
            status = ErrorCode.UNKNOWN_INTERNAL_ERROR.httpStatus,
            errorCode = ErrorCode.UNKNOWN_INTERNAL_ERROR.errorCode,
            message = ErrorCode.UNKNOWN_INTERNAL_ERROR.defaultMessage,
            timestamp = System.currentTimeMillis()
        )
        return ResponseEntity(errorResponse, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    }
}