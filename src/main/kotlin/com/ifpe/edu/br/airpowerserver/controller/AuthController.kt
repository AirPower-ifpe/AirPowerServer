package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.dto.ErrorResponse
import com.ifpe.edu.br.airpowerserver.dto.LoginRequest
import com.ifpe.edu.br.airpowerserver.dto.RefreshRequest
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
    private val tokenService: TokenService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest
    ): ResponseEntity<Any> {
        try {
            val thingsBoardUserId = thingsBoardAuthService.authenticate(loginRequest)
            val tokens = tokenService.generateTokens(thingsBoardUserId)
            return ResponseEntity.ok(tokens)
        } catch (e: Exception) {
            return ResponseEntity.status(500)
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
        val userId = tokenService.validateAndGetUserIdFromRefreshToken(refreshRequest.refreshToken)
            ?: return ResponseEntity.status(401)
                .body(mapOf("error" to "Refresh token inválido ou expirado"))
        tokenService.invalidateRefreshToken(refreshRequest.refreshToken)
        val newTokens = tokenService.generateTokens(userId)
        return ResponseEntity.ok(newTokens)
    }
}