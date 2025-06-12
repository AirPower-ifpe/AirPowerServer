package com.ifpe.edu.br.airpowerserver.controller

import com.auth0.jwt.JWT
import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.dto.ErrorResponse
import com.ifpe.edu.br.airpowerserver.dto.auth.LoginRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.RefreshRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.ThingsBoardLoginResponse
import com.ifpe.edu.br.airpowerserver.entity.airpower.PersistToken
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardAuthService
import com.ifpe.edu.br.airpowerserver.service.TokenService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/test/api/v1/user/auth")
class AuthController(
    private val thingsBoardAuthService: ThingsBoardAuthService,
    private val tokenService: TokenService,
    private val tokenRepository: TokenRepository
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest
    ): ResponseEntity<Any> {
        try {
            logger.info("Logging into user {}", loginRequest.username)
            val thingsBoardIncomeToken = thingsBoardAuthService.authenticate(loginRequest)
            val thingsBoardUserId = getUserIdFromToken(thingsBoardIncomeToken)

            var thingsBoardPersistedToken =
                tokenRepository.findByUserIdAndScope(thingsBoardUserId, Constants.Scope.THINGS_BOARD)

            if (thingsBoardPersistedToken != null) {
                thingsBoardPersistedToken.jwt = thingsBoardIncomeToken.token
                thingsBoardPersistedToken.refreshToken = thingsBoardIncomeToken.refreshToken
            } else {
                thingsBoardPersistedToken =
                    PersistToken(
                        jwt = thingsBoardIncomeToken.token,
                        refreshToken = thingsBoardIncomeToken.refreshToken,
                        userId = thingsBoardUserId,
                        scope = Constants.Scope.THINGS_BOARD
                    )
            }

            var airPowerPersistToken =
                tokenRepository.findByUserIdAndScope(thingsBoardUserId, Constants.Scope.AIR_POWER)
            val generateAirPowerToken = tokenService.generateAirPowerToken(thingsBoardUserId)

            if (airPowerPersistToken != null) {
                airPowerPersistToken.jwt = generateAirPowerToken.accessToken
                airPowerPersistToken.refreshToken = generateAirPowerToken.refreshToken
            } else {
                airPowerPersistToken =
                    PersistToken(
                        jwt = generateAirPowerToken.accessToken,
                        refreshToken = generateAirPowerToken.refreshToken,
                        userId = thingsBoardUserId,
                        scope = Constants.Scope.AIR_POWER
                    )

            }
            tokenRepository.save(airPowerPersistToken)
            tokenRepository.save(thingsBoardPersistedToken)
            return ResponseEntity.ok(generateAirPowerToken)
        } catch (e: Exception) {
            logger.error("Error while logging into user {}", loginRequest.username, e)
            return buildErrorResponse(
                status = 401,
                throwable = e,
                errorCode = Constants.ErrorCodes.AUTHENTICATION_FAILED
            )
        }
    }

    @PostMapping("/token")
    fun refresh(
        @RequestBody refreshRequest: RefreshRequest
    ): ResponseEntity<Any> {
        try {
            logger.info("Refresh token: {}", refreshRequest.refreshToken)
            val storedAirPowerToken = tokenRepository.findByRefreshToken(refreshRequest.refreshToken)
            if (storedAirPowerToken == null) {
                return buildErrorResponse(
                    status = 401,
                    throwable = IllegalAccessException("Token not found"),
                    errorCode = Constants.ErrorCodes.TOKEN_EXPIRED
                )
            }
            val storedThingsBoardToken = tokenRepository.findByUserIdAndScope(
                userId = storedAirPowerToken.userId,
                scope = Constants.Scope.THINGS_BOARD
            )
            if (storedThingsBoardToken == null) {
                return buildErrorResponse(
                    status = 500,
                    throwable = IllegalAccessException("ThingsBoard token not found"),
                    errorCode = Constants.ErrorCodes.TOKEN_EXPIRED
                )
            }

            val thingsBoardRefreshedToken = thingsBoardAuthService
                .updateSession(RefreshRequest(storedThingsBoardToken.refreshToken))

            storedThingsBoardToken.jwt = thingsBoardRefreshedToken.token
            storedThingsBoardToken.refreshToken = thingsBoardRefreshedToken.refreshToken
            tokenRepository.save(storedThingsBoardToken)

            val refreshedAirPowerToken = tokenService.generateAirPowerToken(storedThingsBoardToken.userId)
            storedAirPowerToken.jwt = refreshedAirPowerToken.accessToken
            storedAirPowerToken.refreshToken = refreshedAirPowerToken.refreshToken
            tokenRepository.save(storedAirPowerToken)
            return ResponseEntity.ok(refreshedAirPowerToken)
        } catch (e: Exception) {
            logger.error("Error while refresh token", e)
            return buildErrorResponse(
                status = 401,
                throwable = e,
                errorCode = Constants.ErrorCodes.TOKEN_EXPIRED
            )
        }
    }

    fun getUserIdFromToken(incomingToken: ThingsBoardLoginResponse): String {
        val decodedThingsBoardJWT = JWT.decode(incomingToken.token)
        return decodedThingsBoardJWT.getClaim("userId").asString()
    }

    private fun buildErrorResponse(
        status: Int,
        throwable: Throwable,
        errorCode: Int
    ): ResponseEntity<Any> {
        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status,
                message = "message: $throwable",
                errorCode = errorCode,
                timestamp = Instant.now().toString()
            )
        )
    }
}