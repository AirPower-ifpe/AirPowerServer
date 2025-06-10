package com.ifpe.edu.br.airpowerserver.dto.auth

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
{
    override fun toString(): String {
        return "TokenResponse(accessToken='$accessToken', refreshToken='$refreshToken')"
    }
}