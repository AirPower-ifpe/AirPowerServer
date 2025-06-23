package com.ifpe.edu.br.airpowerserver.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ifpe.edu.br.airpowerserver.config.DownstreamServiceException
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.DefaultResponseErrorHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

@Component
class ServerResponseErrorHandler: DefaultResponseErrorHandler() {

    private val logger = LoggerFactory.getLogger(ServerResponseErrorHandler::class.java)
    private val objectMapper = jacksonObjectMapper()

    override fun handleError(response: ClientHttpResponse) {
        logger.warn("handleError")
        val errorBody = try {
            InputStreamReader(response.body, StandardCharsets.UTF_8).use { reader ->
                BufferedReader(reader).lines().collect(Collectors.joining("\n"))
            }
        } catch (e: Exception) {
            logger.error("Error: ${e.message}")
            e.message
        }

        val tbError = runCatching {
            objectMapper.readValue(errorBody, ErrorResponse::class.java)
        }.getOrNull()
        val airPowerCode = mapThingsBoardErrorCode(tbError?.errorCode)
        logger.error("airPowerCode: {}", airPowerCode.toString())
        throw DownstreamServiceException(airPowerCode, errorBody)
    }

    private fun mapThingsBoardErrorCode(tbCode: Int?): ErrorCode {
        return when (tbCode) {
            10 -> ErrorCode.TB_INVALID_CREDENTIALS
            11 -> ErrorCode.TB_JWT_EXPIRED
            15 -> ErrorCode.TB_REFRESH_TOKEN_EXPIRED
            else -> ErrorCode.TB_GENERIC_ERROR
        }
    }

    override fun getResponseBody(response: ClientHttpResponse): ByteArray {
        logger.warn("getResponseBody ${response.body}")
        return super.getResponseBody(response)
    }
}