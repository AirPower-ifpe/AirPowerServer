package com.ifpe.edu.br.airpowerserver.dto.error

open class ApiException(
    val errorCode: ErrorCode,
    message: String? = null
) : RuntimeException(message ?: errorCode.defaultMessage)