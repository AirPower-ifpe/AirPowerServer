package com.ifpe.edu.br.airpowerserver.dto.error

import com.ifpe.edu.br.airpowerserver.config.Constants

enum class ErrorCode(
    val httpStatus: Int,
    val errorCode: Int,
    val defaultMessage: String
) {
    INVALID_AIRPOWER_TOKEN(401, Constants.ResponseErrorCodes.INVALID_AIRPOWER_TOKEN,"O token de acesso fornecido é inválido ou expirou."),
    INVALID_REFRESH_TOKEN(401, Constants.ResponseErrorCodes.INVALID_REFRESH_TOKEN,"O token de atualização é inválido ou já foi utilizado."),
    ACCESS_DENIED(403, Constants.ResponseErrorCodes.ACCESS_DENIED,"Acesso negado. Você não tem permissão para realizar esta operação."),
    INVALID_ARGUMENTS(400, Constants.ResponseErrorCodes.INVALID_ARGUMENTS,"Argumentos inválidos na requisição."),
    TB_AUTHENTICATION_FAILED(401, Constants.ResponseErrorCodes.TB_AUTHENTICATION_FAILED,"Falha na autenticação com o serviço externo."),
    TB_JWT_EXPIRED(401, Constants.ResponseErrorCodes.TB_JWT_EXPIRED,"A sessão com o serviço externo expirou. Tente novamente."),
    TB_PERMISSION_DENIED(403, Constants.ResponseErrorCodes.TB_PERMISSION_DENIED,"Permissão negada pelo serviço externo."),
    TB_ITEM_NOT_FOUND(404, Constants.ResponseErrorCodes.TB_ITEM_NOT_FOUND,"O recurso solicitado não foi encontrado no serviço externo."),
    TB_TOO_MANY_REQUESTS(429, Constants.ResponseErrorCodes.TB_TOO_MANY_REQUESTS,"Muitas requisições para o serviço externo. Tente mais tarde."),
    TB_GENERIC_ERROR(502, Constants.ResponseErrorCodes.TB_GENERIC_ERROR,"Ocorreu um erro inesperado no serviço externo."),
    UNKNOWN_INTERNAL_ERROR(500, Constants.ResponseErrorCodes.UNKNOWN_INTERNAL_ERROR,"Ocorreu um erro interno inesperado no servidor.");
}
