package com.ifpe.edu.br.airpowerserver.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ifpe.edu.br.airpowerserver.config.AirPowerUserDetailsImpl
import com.ifpe.edu.br.airpowerserver.dto.auth.TokenResponse
import com.ifpe.edu.br.airpowerserver.dto.error.DownstreamServiceException
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class AirPowerTokenService(
    private val tokenRepository: TokenRepository
) {

    private val logger = LoggerFactory.getLogger(AirPowerTokenService::class.java)


    @Value("\${airpower.jwt.secret}")
    private lateinit var airPowerJWTSecret: String

    @Value("\${airpower.jwt.expirationMinutes}")
    private lateinit var airPowerJWTExpirationMinutes: Number

    @Value("\${airpower.refreshToken.expirationMinutes}")
    private lateinit var airPowerRefreshTokenExpirationMinutes: Number

    private val issuer = "AirPowerServer"


    fun generateAirPowerToken(userDetailsImpl: AirPowerUserDetailsImpl): TokenResponse {
        val algorithm = Algorithm.HMAC256(airPowerJWTSecret)
        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withIssuedAt(Date.from(Instant.now()))
            .withSubject(userDetailsImpl.username)
            .withExpiresAt(
                Date(
                    System.currentTimeMillis()
                            + TimeUnit.MINUTES.toMillis(airPowerJWTExpirationMinutes.toLong())
                )
            )
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(userDetailsImpl.username)
            .withIssuedAt(Date.from(Instant.now()))
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(
                Date(
                    System.currentTimeMillis()
                            + TimeUnit.MINUTES.toMillis(airPowerRefreshTokenExpirationMinutes.toLong())
                )
            )
            .sign(algorithm)
        return TokenResponse(accessToken, refreshToken, userDetailsImpl.authorities.toString())
    }

    fun getSubjectFromToken(token: String): String {
        try {
            val algorithm = Algorithm.HMAC256(airPowerJWTSecret)
            return JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token)
                .subject
        } catch (ex: Exception) {
            throw DownstreamServiceException(
                ErrorCode.INVALID_AIRPOWER_TOKEN, "invalid token",
            )
        }
    }
}