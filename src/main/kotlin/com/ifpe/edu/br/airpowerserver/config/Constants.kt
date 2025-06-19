package com.ifpe.edu.br.airpowerserver.config

class Constants {

    object ResponseErrorCodes {
        // --- Autenticação AirPower (1xxx) ---
        const val INVALID_AIRPOWER_TOKEN = 1
        const val INVALID_REFRESH_TOKEN = 11
        const val ACCESS_DENIED = 12

        // --- Erros de Requisição (2xxx) ---
        const val INVALID_ARGUMENTS = 2

        // --- Erros Mapeados do ThingsBoard (3xxx) ---
        const val TB_AUTHENTICATION_FAILED = 31
        const val TB_JWT_EXPIRED = 32
        const val TB_PERMISSION_DENIED = 33
        const val TB_ITEM_NOT_FOUND = 34
        const val TB_TOO_MANY_REQUESTS = 35
        const val TB_GENERIC_ERROR = 36

        // --- Erros Genéricos (9xxx) ---
        const val UNKNOWN_INTERNAL_ERROR = 9

    }

    object Scope {
        const val AIR_POWER = 0
        const val THINGS_BOARD = 1
    }
}