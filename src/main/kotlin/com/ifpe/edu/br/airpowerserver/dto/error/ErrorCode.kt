package com.ifpe.edu.br.airpowerserver.dto.error

import com.ifpe.edu.br.airpowerserver.config.Constants

enum class ErrorCode(
    val httpStatus: Int,
    val errorCode: Int,
    val defaultMessage: String
) {
    INVALID_AIRPOWER_TOKEN(401, Constants.ResponseErrorCodes.AP_JWT_EXPIRED,"O token de acesso fornecido é inválido ou expirou."),
    INVALID_REFRESH_TOKEN(401, Constants.ResponseErrorCodes.AP_REFRESH_TOKEN_EXPIRED,"O token de atualização é inválido ou já foi utilizado."),
    TB_INVALID_CREDENTIALS(401, Constants.ResponseErrorCodes.TB_INVALID_CREDENTIALS, "Usuário ou senha incorretos."),
    TB_JWT_EXPIRED(401, Constants.ResponseErrorCodes.TB_JWT_EXPIRED,"A sessão com o serviço externo expirou. Tente novamente."),
    TB_REFRESH_TOKEN_EXPIRED(401, Constants.ResponseErrorCodes.TB_REFRESH_TOKEN_EXPIRED,"O refresh token expirou. Tente novamente."),
    TB_GENERIC_ERROR(502, Constants.ResponseErrorCodes.TB_GENERIC_ERROR,"Ocorreu um erro inesperado no serviço externo."),
    UNKNOWN_INTERNAL_ERROR(500, Constants.ResponseErrorCodes.UNKNOWN_INTERNAL_ERROR,"Ocorreu um erro interno inesperado no servidor.");
}