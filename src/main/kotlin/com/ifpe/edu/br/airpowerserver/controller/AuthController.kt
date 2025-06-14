package com.ifpe.edu.br.airpowerserver.controller

import com.auth0.jwt.JWT
import com.ifpe.edu.br.airpowerserver.config.AirPowerUserDetailsImpl
import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.config.RoleName
import com.ifpe.edu.br.airpowerserver.dto.ErrorResponse
import com.ifpe.edu.br.airpowerserver.dto.auth.LoginRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.RefreshRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.ThingsBoardLoginResponse
import com.ifpe.edu.br.airpowerserver.entity.airpower.AirPowerUser
import com.ifpe.edu.br.airpowerserver.entity.airpower.PersistToken
import com.ifpe.edu.br.airpowerserver.entity.airpower.Role
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardAuthService
import com.ifpe.edu.br.airpowerserver.service.AirPowerTokenService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val thingsBoardAuthService: ThingsBoardAuthService,
    private val tokenService: AirPowerTokenService,
    private val tokenRepository: TokenRepository,
    private val airPowerUserRepository: AirPowerUserRepository,
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest
    ): ResponseEntity<Any> {
        try {
            logger.info("Logging into user {}", loginRequest.username)
            val thingsBoardIncomeToken = thingsBoardAuthService.authenticate(loginRequest)
            logger.info("thingsBoardIncomeToken {}", thingsBoardIncomeToken)
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

            val airPowerUser = AirPowerUser()
            airPowerUser.id = thingsBoardUserId
            airPowerUser.email = loginRequest.username
            airPowerUser.password = loginRequest.password
            airPowerUser.roles = extractRolesFromJwt(thingsBoardIncomeToken)

            var airPowerPersistToken =
                tokenRepository.findByUserIdAndScope(thingsBoardUserId, Constants.Scope.AIR_POWER)
            val generateAirPowerToken = tokenService.generateAirPowerToken(AirPowerUserDetailsImpl(airPowerUser))

            if (airPowerPersistToken != null) {
                airPowerPersistToken.jwt = generateAirPowerToken.token
                airPowerPersistToken.refreshToken = generateAirPowerToken.refreshToken
            } else {
                airPowerPersistToken =
                    PersistToken(
                        jwt = generateAirPowerToken.token,
                        refreshToken = generateAirPowerToken.refreshToken,
                        userId = thingsBoardUserId,
                        scope = Constants.Scope.AIR_POWER
                    )

            }
            tokenRepository.save(airPowerPersistToken)
            tokenRepository.save(thingsBoardPersistedToken)
            airPowerUserRepository.save(airPowerUser)
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

            val storedAirPowerUser = airPowerUserRepository.findById(storedAirPowerToken.userId)

            val refreshedAirPowerToken = tokenService.generateAirPowerToken(AirPowerUserDetailsImpl(storedAirPowerUser.get()))
            storedAirPowerToken.jwt = refreshedAirPowerToken.token
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

    fun getUserIdFromToken(incomingToken: ThingsBoardLoginResponse): UUID {
        val decodedThingsBoardJWT = JWT.decode(incomingToken.token)
        return UUID.fromString(decodedThingsBoardJWT.getClaim("userId").asString())
    }

    fun extractRolesFromJwt(incomingToken: ThingsBoardLoginResponse): MutableList<Role> {
        val decodedJWT = JWT.decode(incomingToken.token)
        val scopes = decodedJWT.getClaim("scopes").asList(String::class.java) ?: return mutableListOf()
        return scopes.mapNotNull { scope ->
            runCatching {
                val roleName = RoleName.valueOf(scope)
                Role().apply {
                    name = roleName
                }
            }.getOrNull()
        }.toMutableList()
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