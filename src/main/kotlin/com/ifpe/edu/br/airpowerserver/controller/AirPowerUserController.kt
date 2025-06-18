package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.dto.ErrorResponse
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardUserService
import io.ktor.server.plugins.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("api/v1/user")
class AirPowerUserController(
    private val airPowerUserRepository: AirPowerUserRepository,
    private val thingsBoardService: ThingsBoardUserService
) {

    private val logger = LoggerFactory.getLogger(AirPowerUserController::class.java)

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<Any> {
        try {
            logger.info("getCurrentUser()")
            val authentication = SecurityContextHolder.getContext().authentication
            val username = authentication?.name
            return if (username == null) {
                return buildErrorResponse(
                    404,
                    NotFoundException("User not found"),
                    Constants.ResponseErrorCodes.TOKEN_EXPIRED
                )
            } else {
                val storedAirPowerUser = airPowerUserRepository.findByEmail(username)
                val thingsBoardUserFromApi = thingsBoardService.getCurrentUser(storedAirPowerUser.id!!)
                ResponseEntity.ok(thingsBoardUserFromApi)
            }
        } catch (e: Exception) {
            logger.error("unexpected error", e)
            return buildErrorResponse(
                500,
                e,
                Constants.ResponseErrorCodes.TOKEN_EXPIRED
            )
        }
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