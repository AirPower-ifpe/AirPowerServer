package com.ifpe.edu.br.airpowerserver.dto.auth

data class LoginRequest(
    val username: String,
    val password: String
)
{
    override fun toString(): String {
        return "LoginRequest(username='$username', password='$password')"
    }
}