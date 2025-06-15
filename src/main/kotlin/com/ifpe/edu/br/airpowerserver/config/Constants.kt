package com.ifpe.edu.br.airpowerserver.config

class Constants {
    object ErrorCodes {
        const val AUTHENTICATION_FAILED = 10
        const val TOKEN_EXPIRED = 11
        const val SERVER_AIRPOWER_ERROR = 12
        const val SERVER_THINGSBOARD_ERROR = 13
    }

    object Scope {
        const val AIR_POWER = 0
        const val THINGS_BOARD = 1
    }
}