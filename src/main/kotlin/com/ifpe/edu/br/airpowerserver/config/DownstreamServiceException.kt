package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode

class DownstreamServiceException(
    errorCode: ErrorCode,
    val downstreamResponseBody: String?
) : ApiException(errorCode, "Erro no serviço downstream. Causa: $downstreamResponseBody")