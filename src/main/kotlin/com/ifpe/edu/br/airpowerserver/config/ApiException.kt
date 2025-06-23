package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode

open class ApiException(
    val errorCode: ErrorCode,
    message: String? = null
) : RuntimeException(message ?: errorCode.defaultMessage)