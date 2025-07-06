package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.config.Constants
import com.ifpe.edu.br.airpowerserver.repository.airpower.TokenRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.*

@Service
class ThingsBoardServiceUtil(
    private val tokenRepository: TokenRepository
) {

    fun getAuthHeaders(userId: UUID): HttpHeaders {
        val tbToken = tokenRepository.findByUserIdAndScope(userId, Constants.Scope.THINGS_BOARD)
            ?: throw IllegalStateException("ThingsBoard token not found for user ID: $userId")
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Authorization", "Bearer ${tbToken.jwt}")
        }
    }
}