package com.ifpe.edu.br.airpowerserver.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ifpe.edu.br.airpowerserver.dto.auth.TokenResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class TokenService {

    @Value("\${airpower.jwt.secret}")
    private lateinit var airPowerJWTSecret: String

    @Value("\${airpower.jwt.expirationMinutes}")
    private lateinit var airPowerJWTExpirationMinutes: Number

    @Value("\${airpower.refreshToken.expirationDays}")
    private lateinit var airPowerRefreshTokenExpirationMinutes: Number

    private val issuer = "AirPowerServer"

    // Armazenamento em memória. Para produção, considere usar Redis.
    private val validRefreshTokens = ConcurrentHashMap<String, String>()

    fun generateTokens(userId: String): TokenResponse {
        val algorithm = Algorithm.HMAC256(airPowerJWTSecret)

        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis()
                    + TimeUnit.MINUTES.toMillis(airPowerJWTExpirationMinutes.toLong())))
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(Date(System.currentTimeMillis()
                    + TimeUnit.DAYS.toMillis(airPowerRefreshTokenExpirationMinutes.toLong())))
            .sign(algorithm)

        validRefreshTokens[refreshToken] = userId
        return TokenResponse(accessToken, refreshToken)
    }

    fun validateAndGetUserIdFromRefreshToken(token: String): String {
        try {
            val storedUserId = validRefreshTokens[token] ?: throw IllegalStateException("invalid token")
            val algorithm = Algorithm.HMAC256(airPowerJWTSecret)
            val verifier = JWT.require(algorithm).withIssuer(issuer).build()
            val decodedJWT = verifier.verify(token)
            val tokenUserId = decodedJWT.getClaim("userId").asString()
            return if (tokenUserId == storedUserId) storedUserId else throw IllegalStateException("invalid token")
        } catch (e: Exception) {
            validRefreshTokens.remove(token)
            throw IllegalStateException("${e.message}")
        }
    }

    fun invalidateRefreshToken(token: String) {
        validRefreshTokens.remove(token)
    }
}