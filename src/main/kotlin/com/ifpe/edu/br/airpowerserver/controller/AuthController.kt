package com.ifpe.edu.br.airpowerserver.controller

import com.auth0.jwt.JWT
import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.dto.ErrorResponse
import com.ifpe.edu.br.airpowerserver.dto.auth.LoginRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.RefreshRequest
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
            val thingsBoardToken = thingsBoardAuthService.authenticate(loginRequest)
            val decodedThingsBoardJWT = JWT.decode(thingsBoardToken.token)
            val thingsBoardUserId = decodedThingsBoardJWT.getClaim("userId").asString()
            logger.info("user Id: {}", thingsBoardUserId) // todo delete it

            var thingsBoardPersistedToken =
                tokenRepository.findByUserIdAndScope(thingsBoardUserId, Constants.Scope.THINGS_BOARD)

            if (thingsBoardPersistedToken != null) {
                thingsBoardPersistedToken.jwt = thingsBoardToken.token
                thingsBoardPersistedToken.refreshToken = thingsBoardToken.refreshToken
            } else {
                thingsBoardPersistedToken =
                    PersistToken(
                        jwt = thingsBoardToken.token,
                        refreshToken = thingsBoardToken.refreshToken,
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
            return ResponseEntity.status(401)
                .body(
                    ErrorResponse(
                        status = 401,
                        message = "message: ${e.message}",
                        errorCode = Constants.ErrorCodes.AUTHENTICATION_FAILED,
                        timestamp = Instant.now().toString()
                    )
                )
        }
    }

    @PostMapping("/token")
    fun refresh(@RequestBody refreshRequest: RefreshRequest): ResponseEntity<Any> {
        try {
            logger.info("Refresh token: {}", refreshRequest.refreshToken)
            val tbuserId = thingsBoardAuthService.updateSession(refreshRequest)


            //val userId = tokenService.validateAndGetUserIdFromRefreshToken(refreshRequest.refreshToken)
//            tokenService.invalidateRefreshToken(refreshRequest.refreshToken)
//            val newTokens = tokenService.generateAirPowerToken(userId)
            return ResponseEntity.ok("newTokens")
        } catch (e: Exception) {
            logger.error("Error while refresh token", e)
            return ResponseEntity.status(401)
                .body(
                    ErrorResponse(
                        status = 401,
                        message = "message: ${e.message}",
                        errorCode = Constants.ErrorCodes.TOKEN_EXPIRED,
                        timestamp = Instant.now().toString()
                    )
                )
        }
    }
}