package com.ifpe.edu.br.airpowerserver.dto.auth

data class ThingsBoardLoginResponse(
    val token: String,
    val refreshToken: String
)
{
    override fun toString(): String {
        return "ThingsBoardLoginResponse(token='$token', refreshToken='$refreshToken')"
    }
}