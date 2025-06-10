package com.ifpe.edu.br.airpowerserver.service

import com.auth0.jwt.JWT
import com.ifpe.edu.br.airpowerserver.dto.auth.LoginRequest
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


    fun authenticate(loginRequest: LoginRequest): String {
        try {
            val loginUrl = "$thingsBoardApiUrl/api/auth/login"
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val requestEntity = HttpEntity(loginRequest, headers)
            val response = restTemplate.postForEntity(loginUrl, requestEntity, ThingsBoardLoginResponse::class.java)
            if (response.statusCode.is2xxSuccessful) {
                val tbToken = response.body?.token
                val decodedJWT = JWT.decode(tbToken)
                return decodedJWT.getClaim("userId").asString()
            } else {
                throw IllegalStateException("Authentication failed: ${response.statusCode}")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Authentication failed: ${e.message}")
        }
    }
}
