package com.ifpe.edu.br.airpowerserver.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ifpe.edu.br.airpowerserver.dto.auth.TokenResponse
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
class TokenService(
    private val tokenRepository: TokenRepository
) {

    private val logger = LoggerFactory.getLogger(TokenService::class.java)


    @Value("\${airpower.jwt.secret}")
    private lateinit var airPowerJWTSecret: String

    @Value("\${airpower.jwt.expirationMinutes}")
    private lateinit var airPowerJWTExpirationMinutes: Number

    @Value("\${airpower.refreshToken.expirationDays}")
    private lateinit var airPowerRefreshTokenExpirationMinutes: Number

    private val issuer = "AirPowerServer"

    // Armazenamento em memória. Para produção, considere usar Redis.
    private val validRefreshTokens = ConcurrentHashMap<String, String>()

    fun generateAirPowerToken(userId: String): TokenResponse {
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
        return TokenResponse(accessToken, refreshToken)
    }

    fun validateAndGetUserIdFromRefreshToken(token: String): String {
        try {
//            val refreshToken = tokenRepository.findByJwt(token)
//            if (refreshToken.expiryDate.isBefore(Instant.now())) {
//                tokenRepository.delete(refreshToken)
//                throw IllegalStateException("Refresh token expirado.")
//            }
            return "refreshToken.userId"
        } catch (e: Exception) {
            validRefreshTokens.remove(token)
            throw IllegalStateException("${e.message}")
        }
    }

    fun validateJwtToken(authToken: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(airPowerJWTSecret).build().parseClaimsJws(authToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun invalidateRefreshToken(token: String) {
        try {
            //val token = tokenRepository.findByToken(token)
            //tokenRepository.delete(token)
        } catch (e: Exception) {
            logger.error("Refresh token nao encontrado. ${e.message}")
        }
    }
}