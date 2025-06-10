package com.ifpe.edu.br.airpowerserver.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ifpe.edu.br.airpowerserver.dto.TokenResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class TokenService {

    // Carrega o segredo do 'application.properties'. Muito mais seguro!
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    private val issuer = "AirPowerServer"

    // Armazenamento em memória. Para produção, considere usar Redis.
    private val validRefreshTokens = ConcurrentHashMap<String, String>()

    fun generateTokens(userId: String): TokenResponse {
        val algorithm = Algorithm.HMAC256(jwtSecret)

        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15))) // 15 minutos
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7))) // 7 dias
            .sign(algorithm)

        validRefreshTokens[refreshToken] = userId
        return TokenResponse(accessToken, refreshToken)
    }

    fun validateAndGetUserIdFromRefreshToken(token: String): String? {
        // Se o token nem está na nossa lista, já é inválido.
        val storedUserId = validRefreshTokens[token] ?: return null

        try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()
            val decodedJWT = verifier.verify(token)

            // Confirma se o usuário do token é o mesmo que armazenamos
            val tokenUserId = decodedJWT.getClaim("userId").asString()
            return if (tokenUserId == storedUserId) storedUserId else null
        } catch (e: Exception) {
            // Se a validação falhar (expirado, assinatura inválida), removemos o token.
            validRefreshTokens.remove(token)
            return null
        }
    }

    fun invalidateRefreshToken(token: String) {
        validRefreshTokens.remove(token)
    }
}