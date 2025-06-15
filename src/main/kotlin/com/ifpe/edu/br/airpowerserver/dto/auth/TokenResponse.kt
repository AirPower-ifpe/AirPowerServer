package com.ifpe.edu.br.airpowerserver.dto.auth

data class TokenResponse(
    val token: String,
    val refreshToken: String,
    val scope : String,
)
{
    override fun toString(): String {
        return "TokenResponse(token='$token', refreshToken='$refreshToken', scope='$scope')"
    }
}