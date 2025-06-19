package com.ifpe.edu.br.airpowerserver.controller

import com.auth0.jwt.JWT
import com.ifpe.edu.br.airpowerserver.config.AESUtils
import com.ifpe.edu.br.airpowerserver.config.AirPowerUserDetailsImpl
import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.config.RoleName
import com.ifpe.edu.br.airpowerserver.dto.ErrorResponse
import com.ifpe.edu.br.airpowerserver.dto.auth.LoginRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.RefreshRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.ThingsBoardLoginResponse
import com.ifpe.edu.br.airpowerserver.dto.error.DownstreamServiceException
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.entity.airpower.AirPowerUser
import com.ifpe.edu.br.airpowerserver.entity.airpower.PersistToken
import com.ifpe.edu.br.airpowerserver.entity.airpower.Role
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.repository.airpower.RoleRepository
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import com.ifpe.edu.br.airpowerserver.service.AirPowerTokenService
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardAuthService
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val thingsBoardAuthService: ThingsBoardAuthService,
    private val tokenService: AirPowerTokenService,
    private val tokenRepository: TokenRepository,
    private val airPowerUserRepository: AirPowerUserRepository,
    private val roleRepository: RoleRepository,
    private val aesUtils: AESUtils
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    @Transactional
    fun login(
        @RequestBody loginRequest: LoginRequest
    ): ResponseEntity<Any> {
        logger.info("Logging into user {}", loginRequest.username)
        val thingsBoardIncomeToken = thingsBoardAuthService.authenticate(loginRequest)
        val thingsBoardUserId = getUserIdFromToken(thingsBoardIncomeToken)

        val airPowerUser = airPowerUserRepository.findById(thingsBoardUserId).orElse(AirPowerUser()).apply {
            id = thingsBoardUserId
            email = loginRequest.username
            password = aesUtils.encrypt(loginRequest.password)
            role = findOrCreateRoles(thingsBoardIncomeToken)[0]
        }

        airPowerUserRepository.save(airPowerUser)

        val thingsBoardPersistedToken =
            tokenRepository.findByUserIdAndScope(thingsBoardUserId, Constants.Scope.THINGS_BOARD)
                ?.apply {
                    jwt = thingsBoardIncomeToken.token
                    refreshToken = thingsBoardIncomeToken.refreshToken
                }
                ?: PersistToken(
                    jwt = thingsBoardIncomeToken.token,
                    refreshToken = thingsBoardIncomeToken.refreshToken,
                    userId = thingsBoardUserId,
                    scope = Constants.Scope.THINGS_BOARD
                )

        tokenRepository.save(thingsBoardPersistedToken)

        val generateAirPowerToken = tokenService.generateAirPowerToken(AirPowerUserDetailsImpl(airPowerUser))
        val airPowerPersistToken =
            tokenRepository.findByUserIdAndScope(thingsBoardUserId, Constants.Scope.AIR_POWER)
                ?.apply {
                    jwt = generateAirPowerToken.token
                    refreshToken = generateAirPowerToken.refreshToken
                }
                ?: PersistToken(
                    jwt = generateAirPowerToken.token,
                    refreshToken = generateAirPowerToken.refreshToken,
                    userId = thingsBoardUserId,
                    scope = Constants.Scope.AIR_POWER
                )
        tokenRepository.save(airPowerPersistToken)
        return ResponseEntity.ok(generateAirPowerToken)
    }

    @PostMapping("/token")
    fun refresh(
        @RequestBody refreshRequest: RefreshRequest
    ): ResponseEntity<Any> {
        logger.info("Refresh token: {}", refreshRequest.refreshToken)
        val storedAirPowerToken = tokenRepository.findByRefreshToken(refreshRequest.refreshToken)
        if (storedAirPowerToken == null) {
            throw DownstreamServiceException(
                ErrorCode.INVALID_REFRESH_TOKEN,
                "Token not found"
            )
        }
        val storedThingsBoardToken = tokenRepository.findByUserIdAndScope(
            userId = storedAirPowerToken.userId,
            scope = Constants.Scope.THINGS_BOARD
        )
        if (storedThingsBoardToken == null) {
            throw DownstreamServiceException(
                ErrorCode.INVALID_REFRESH_TOKEN,
                "ThingsBoard token not found"
            )
        }

        val thingsBoardRefreshedToken = thingsBoardAuthService
            .updateSession(RefreshRequest(storedThingsBoardToken.refreshToken))

        storedThingsBoardToken.jwt = thingsBoardRefreshedToken.token
        storedThingsBoardToken.refreshToken = thingsBoardRefreshedToken.refreshToken
        tokenRepository.save(storedThingsBoardToken)

        val storedAirPowerUser = airPowerUserRepository.findById(storedAirPowerToken.userId)

        logger.warn("storedAirPowerUser ${storedAirPowerUser.get()}")

        val refreshedAirPowerToken =
            tokenService.generateAirPowerToken(AirPowerUserDetailsImpl(storedAirPowerUser.get()))
        storedAirPowerToken.jwt = refreshedAirPowerToken.token
        storedAirPowerToken.refreshToken = refreshedAirPowerToken.refreshToken
        tokenRepository.save(storedAirPowerToken)
        return ResponseEntity.ok(refreshedAirPowerToken)
    }

    private fun findOrCreateRoles(incomingToken: ThingsBoardLoginResponse): MutableList<Role> {
        val decodedJWT = JWT.decode(incomingToken.token)
        val scopes = decodedJWT.getClaim("scopes").asList(String::class.java) ?: return mutableListOf()

        return scopes.mapNotNull { scope ->
            runCatching { RoleName.valueOf(scope) }.getOrNull()
        }.map { roleName ->
            roleRepository.findByName(roleName).orElseGet {
                logger.info("Role '{}' not found, creating a new one.", roleName)
                val newRole = Role().apply { name = roleName }
                roleRepository.save(newRole)
            }
        }.toMutableList()
    }


    fun getUserIdFromToken(incomingToken: ThingsBoardLoginResponse): UUID {
        val decodedThingsBoardJWT = JWT.decode(incomingToken.token)
        return UUID.fromString(decodedThingsBoardJWT.getClaim("userId").asString())
    }
}