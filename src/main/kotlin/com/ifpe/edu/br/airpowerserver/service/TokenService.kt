package com.ifpe.edu.br.airpowerserver.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ifpe.edu.br.airpowerserver.dto.auth.TokenResponse
import com.ifpe.edu.br.airpowerserver.entity.airpower.RefreshToken
import com.ifpe.edu.br.airpowerserver.repository.airpower.AuthRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class TokenService(
    private val refreshTokenRepository: AuthRepository
) {

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
            .withExpiresAt(
                Date(
                    System.currentTimeMillis()
                            + TimeUnit.MINUTES.toMillis(airPowerJWTExpirationMinutes.toLong())
                )
            )
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(
                Date(
                    System.currentTimeMillis()
                            + TimeUnit.DAYS.toMillis(airPowerRefreshTokenExpirationMinutes.toLong())
                )
            )
            .sign(algorithm)

        val refreshTokenEntity = RefreshToken(
            token = refreshToken,
            userId = userId,
            expiryDate = Instant.now().plus(airPowerRefreshTokenExpirationMinutes.toLong(), ChronoUnit.DAYS)
        )

        refreshTokenRepository.save(refreshTokenEntity)
        return TokenResponse(accessToken, refreshToken)
    }

    fun validateAndGetUserIdFromRefreshToken(token: String): String {
        try {
            val refreshTokenEntity = refreshTokenRepository.findByToken(token)
            if (refreshTokenEntity.expiryDate.isBefore(Instant.now())) {
                refreshTokenRepository.delete(refreshTokenEntity)
                throw IllegalStateException("Refresh token expirado.")
            }
            return refreshTokenEntity.userId
        } catch (e: Exception) {
            validRefreshTokens.remove(token)
            throw IllegalStateException("${e.message}")
        }
    }

    fun invalidateRefreshToken(token: String) {
        validRefreshTokens.remove(token)
    }
}