package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.auth.LoginRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.RefreshRequest
import com.ifpe.edu.br.airpowerserver.dto.auth.ThingsBoardLoginResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class ThingsBoardAuthService(private val restTemplate: RestTemplate) {

    @Value("\${thingsboard.api.url}")
    private lateinit var thingsBoardApiUrl: String

    private val logger = LoggerFactory.getLogger(ThingsBoardAuthService::class.java)

    fun authenticate(
        loginRequest: LoginRequest
    ): ThingsBoardLoginResponse {
        try {
            logger.info("authenticate: {}", loginRequest)
            val loginUrl = "$thingsBoardApiUrl/api/auth/login"
            val requestEntity = HttpEntity(loginRequest, getHeader())
            return performRequest(loginUrl, requestEntity)
        } catch (e: Exception) {
            throw IllegalStateException("Authentication failed: ${e.message}")
        }
    }

    fun updateSession(
        refreshToken: RefreshRequest
    ): ThingsBoardLoginResponse {
        try {
            logger.info("updateSession: {}", refreshToken)
            val refreshTokenUrl = "$thingsBoardApiUrl/api/auth/token"
            val requestEntity = HttpEntity(refreshToken, getHeader())
            return performRequest(refreshTokenUrl, requestEntity)
        } catch (e: Exception) {
            throw IllegalStateException("Authentication failed: ${e.message}")
        }
    }

    fun <T> performRequest(
        url: String,
        requestEntity: HttpEntity<T>
    ): ThingsBoardLoginResponse {
        val response =
            restTemplate.postForEntity(url, requestEntity, ThingsBoardLoginResponse::class.java)
        if (response.statusCode.is2xxSuccessful) {
            if (response.body == null) {
                throw IllegalStateException("ThingsBoardLoginResponse was null")
            } else {
                return response.body!!
            }
        } else {
            throw IllegalStateException("Authentication failed: ${response.statusCode}")
        }
    }

    fun getHeader(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
    }
}
