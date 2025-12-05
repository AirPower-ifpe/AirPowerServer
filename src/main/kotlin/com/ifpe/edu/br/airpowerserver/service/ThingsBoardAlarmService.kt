package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.config.toAirPowerAlarmInfo
import com.ifpe.edu.br.airpowerserver.dto.alarms.APAlarmInfo
import com.ifpe.edu.br.airpowerserver.dto.alarms.TBAlarmResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class ThingsBoardAlarmService(
    private val restTemplate: RestTemplate,
    private val tbServiceUtil: ThingsBoardServiceUtil
) {
    @Value("\${thingsboard.api.url}")
    private lateinit var thingsBoardApiUrl: String

    private val logger = LoggerFactory.getLogger(ThingsBoardAlarmService::class.java)

    fun getAllAlarmInfoForUserId(userId: UUID): List<APAlarmInfo> {
        try {
            val url = "$thingsBoardApiUrl/api/alarms${getAlarmQuery(userId = userId, status = "ACTIVE_UNACK")}"
            logger.debug("Requesting alarms from URL: $url")
            val requestEntity = HttpEntity<String>(tbServiceUtil.getAuthHeaders(userId))
            val response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, TBAlarmResponse::class.java)
            if (response.statusCode.is2xxSuccessful && response.body != null) {
                return response.body!!.toAirPowerAlarmInfo()
            } else {
                logger.error("Falha ao buscar alarmes. Status: {}", response.statusCode)
                throw IllegalStateException("Falha ao buscar alarmes no ThingsBoard: ${response.statusCode}")
            }
        } catch (e: Exception) {
            logger.error("Exceção ao chamar a API do ThingsBoard para buscar alarmes", e)
            throw IllegalStateException("Falha na comunicação com o serviço do ThingsBoard: ${e.message}")
        }
    }

    private fun getAlarmQuery(
        userId: UUID,
        page: Int = 0,
        pageSize: Int = 100,
        searchStatus: String? = null,
        status: String? = null,
        assigneeId: UUID? = null,
        textSearch: String? = null,
        sortProperty: String? = null,
        sortOrder: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        fetchOriginator: Boolean? = null
    ): String {
        val queryParams = mutableListOf<String>()

        userId.let { queryParams.add("userId=$it") }
        searchStatus?.let { queryParams.add("searchStatus=$it") }
        status?.let { queryParams.add("status=$it") }
        assigneeId?.let { queryParams.add("assigneeId=$it") }
        textSearch?.let { queryParams.add("textSearch=$it") }
        sortProperty?.let { queryParams.add("sortProperty=$it") }
        sortOrder?.let { queryParams.add("sortOrder=$it") }
        startTime?.let { queryParams.add("startTime=$it") }
        endTime?.let { queryParams.add("endTime=$it") }
        fetchOriginator?.let { queryParams.add("fetchOriginator=$it") }

        // sempre obrigatórios
        queryParams.add("page=$page")
        queryParams.add("pageSize=$pageSize")

        return if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
    }

    /**
     * Retrieves alarms that are not acknowledged.
     *
     * This function filters alarms on the server-side by their 'acknowledged' status.
     * It returns alarms where 'acknowledged' is false, regardless of their 'cleared' status.
     *
     * @param userId The UUID of the user.
     * @return A list of [APAlarmInfo] for unacknowledged alarms.
     */
    fun getUnacknowledgedAlarms(userId: UUID): List<APAlarmInfo> {
        try {
            val url = "$thingsBoardApiUrl/api/alarms${getAlarmQuery(userId, searchStatus = "UNACK")}"
            logger.debug("Requesting unacknowledged alarms from URL: $url")
            val requestEntity = HttpEntity<String>(tbServiceUtil.getAuthHeaders(userId))
            val response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, TBAlarmResponse::class.java)
            return if (response.statusCode.is2xxSuccessful && response.body != null) {
                response.body!!.toAirPowerAlarmInfo()
            } else {
                logger.error("Failed to fetch unacknowledged alarms. Status: {}", response.statusCode)
                throw IllegalStateException("Failed to fetch unacknowledged alarms from ThingsBoard: ${response.statusCode}")
            }
        } catch (e: Exception) {
            logger.error("Exception when calling ThingsBoard API to fetch unacknowledged alarms", e)
            throw IllegalStateException("Failed to communicate with ThingsBoard service: ${e.message}")
        }
    }
}