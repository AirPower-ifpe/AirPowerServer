package com.ifpe.edu.br.airpowerserver.dto.error

class DownstreamServiceException(
    errorCode: ErrorCode,
    val downstreamResponseBody: String?
) : ApiException(errorCode, "Erro no serviço downstream. Causa: $downstreamResponseBody")
