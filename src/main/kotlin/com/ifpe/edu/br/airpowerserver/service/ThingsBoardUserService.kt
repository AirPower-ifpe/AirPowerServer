package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.dto.ThingsBoardUser
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class ThingsBoardUserService(
    private val restTemplate: RestTemplate,
    private val tokenRepository: TokenRepository
) {
    @Value("\${thingsboard.api.url}")
    private lateinit var thingsBoardApiUrl: String

    private val logger = LoggerFactory.getLogger(ThingsBoardUserService::class.java)

    fun getCurrentUser(userId: UUID): ThingsBoardUser {
        logger.info("getCurrentUser ID: {}", userId)
        val tbToken = tokenRepository.findByUserIdAndScope(userId, Constants.Scope.THINGS_BOARD)
            ?: throw IllegalStateException("ThingsBoard token not found for user ID: $userId")

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Authorization", "Bearer ${tbToken.jwt}")
        }

        val url = "$thingsBoardApiUrl/api/auth/user"
        val requestEntity = HttpEntity<String>(headers)

        try {
            val response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, ThingsBoardUser::class.java)
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                logger.error("chegou aquiii {}", response.statusCode)
                return response.body!!
            } else {
                logger.error("Falha ao buscar utilizador do ThingsBoard. Status: {}", response.statusCode)
                throw IllegalStateException("Falha na requisição ao ThingsBoard: ${response.statusCode}")
            }
        } catch (e: Exception) {
            logger.error("Exceção ao chamar a API do ThingsBoard para buscar utilizador", e)
            throw IllegalStateException("Falha na comunicação com o serviço do ThingsBoard: ${e.message}")
        }
    }
}