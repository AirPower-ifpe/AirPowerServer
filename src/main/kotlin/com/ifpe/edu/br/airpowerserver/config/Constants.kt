package com.ifpe.edu.br.airpowerserver.config

class Constants {

    object ResponseErrorCodes {
        // --- Erros Mapeados do ThingsBoard (2xxx) ---
        const val TB_INVALID_CREDENTIALS = 20
        const val TB_REFRESH_TOKEN_EXPIRED = 21
        const val TB_GENERIC_ERROR = 22
        const val TB_JWT_EXPIRED = 23

        // --- Autenticação AirPower (3xxx) ---
        const val AP_JWT_EXPIRED = 30
        const val AP_REFRESH_TOKEN_EXPIRED = 31
        const val AP_GENERIC_ERROR = 32
        @Deprecated("remove it")
        const val ACCESS_DENIED = 12

        // --- Erros Genéricos (9xxx) ---
        const val UNKNOWN_INTERNAL_ERROR = 9
    }

    object Scope {
        const val AIR_POWER = 0
        const val THINGS_BOARD = 1
    }
}