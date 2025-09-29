package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.ThingsBoardDataResponse
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardConfig
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Service
class ThingsBoardDashboardService(
    private val restTemplate: RestTemplate,
    private val thingsBoardServiceUtil: ThingsBoardServiceUtil,
    @Value("\${thingsboard.api.url}") private val thingsBoardApiUrl: String
){
    private val logger = LoggerFactory.getLogger(ThingsBoardDashboardService::class.java)

    /**
     * Fetches all dashboards available for the tenant associated with the given user.
     */
    fun getDashboardsForUser(userId: UUID): List<DashboardInfo> {
        val url = "$thingsBoardApiUrl/api/tenant/dashboards?page=0&pageSize=1000"
        logger.debug("Requesting dashboards for user {} from URL: {}", userId, url)
        val requestEntity = HttpEntity<String>(thingsBoardServiceUtil.getAuthHeaders(userId))

        try {
            val responseType = object : ParameterizedTypeReference<ThingsBoardDataResponse<DashboardInfo>>() {}
            val response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, responseType)

            return response.body?.data ?: emptyList()
        } catch (e: HttpClientErrorException) {
            logger.error("Error fetching dashboards for user {}: {} - {}", userId, e.statusCode, e.responseBodyAsString)
            throw IllegalStateException("Failed to fetch dashboards from ThingsBoard: ${e.statusCode}")
        } catch (e: Exception) {
            logger.error("An unexpected error occurred while fetching dashboards for user {}: {}", userId, e.message)
            throw e
        }
    }

    /**
     * Fetches the unique device IDs from all widgets within a specific dashboard.
     */
    fun getDeviceIdsFromDashboard(userId: UUID, dashboardId: String): List<String> {
        val url = "$thingsBoardApiUrl/api/dashboard/$dashboardId"
        logger.debug("Requesting dashboard details for user {} from URL: {}", userId, url)
        val requestEntity = HttpEntity<String>(thingsBoardServiceUtil.getAuthHeaders(userId))

        try {
            val response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, DashboardConfig::class.java)
            val dashboardConfig = response.body ?: return emptyList()

            val deviceIds = mutableSetOf<String>()

            dashboardConfig.configuration.widgets.values.forEach { widget ->
                widget.config.datasources.forEach { datasource ->
                    // Add entityId or the legacy deviceId, preferring entityId
                    val id = datasource.entityId ?: datasource.deviceId
                    if (id != null) {
                        deviceIds.add(id.toString())
                    }
                }
            }
            return deviceIds.toList()

        } catch (e: HttpClientErrorException) {
            logger.error("Error fetching dashboard details for id {}: {} - {}", dashboardId, e.statusCode, e.responseBodyAsString)
            throw IllegalStateException("Failed to fetch dashboard details from ThingsBoard: ${e.statusCode}")
        } catch (e: Exception) {
            logger.error("An unexpected error occurred while fetching dashboard details for id {}: {}", dashboardId, e.message)
            throw e
        }
    }
}