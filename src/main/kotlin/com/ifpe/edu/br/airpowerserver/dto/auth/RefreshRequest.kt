package com.ifpe.edu.br.airpowerserver.dto.auth

data class RefreshRequest(
    val refreshToken: String
)
{
    override fun toString(): String {
        return "RefreshRequest(refreshToken='$refreshToken')"
    }
}