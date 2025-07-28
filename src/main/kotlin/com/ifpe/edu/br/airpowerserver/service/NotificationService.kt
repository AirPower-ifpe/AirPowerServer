package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.Id
import com.ifpe.edu.br.airpowerserver.dto.ThingsBoardUser
import com.ifpe.edu.br.airpowerserver.dto.notification.AirPowerNotificationItem
import com.ifpe.edu.br.airpowerserver.dto.notification.ThingsBoardNotification
import com.ifpe.edu.br.airpowerserver.dto.notification.ThingsBoardNotificationResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class NotificationService(
    private val restTemplate: RestTemplate,
    private val tbServiceUtil: ThingsBoardServiceUtil
) {
    @Value("\${thingsboard.api.url}")
    private lateinit var thingsBoardApiUrl: String

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Busca as notificações mais recentes para um usuário diretamente da API do ThingsBoard.
     *
     * @param userId O UUID do usuário logado no sistema AirPower.
     * @return Uma lista de [AirPowerNotificationItem] ordenada da mais recente para a mais antiga.
     */
    fun getNotificationsForUser(userId: UUID): List<AirPowerNotificationItem> {
        logger.info("Fetching notifications from ThingsBoard for user: $userId")
        val url = "$thingsBoardApiUrl/api/notifications?page=0&pageSize=50&sortProperty=createdTime&sortOrder=DESC"
        val requestEntity = HttpEntity<String>(tbServiceUtil.getAuthHeaders(userId))
        try {
            val response =
                restTemplate.exchange(url, HttpMethod.GET, requestEntity, ThingsBoardNotificationResponse::class.java)
            val tbNotifications = response.body?.data ?: emptyList()
            return tbNotifications.map { tbNotification ->
                transformTbNotificationToNotificationItem(tbNotification)
            }
        } catch (e: HttpClientErrorException) {
            logger.error("Error fetching notifications from ThingsBoard for user $userId: ${e.statusCode} ${e.responseBodyAsString}")
            return emptyList()
        }
    }

    /**
     * Transforma um objeto de notificação do ThingsBoard em um item de notificação padronizado.
     *
     * @param tbNotification O [ThingsBoardNotification] a ser transformado.
     * @return Um objeto [AirPowerNotificationItem].
     */
    private fun transformTbNotificationToNotificationItem(
        tbNotification: ThingsBoardNotification
    ): AirPowerNotificationItem {
        return AirPowerNotificationItem(
            id = tbNotification.id,
            subject = tbNotification.subject,
            type = tbNotification.type,
            text = tbNotification.text,
            createdTime = tbNotification.createdTime,
            alarmType = tbNotification.info.alarmType,
            alarmId = tbNotification.info.alarmId,
            alarmOriginator = tbNotification.info.alarmOriginator,
            alarmOriginatorName = tbNotification.info.alarmOriginatorName,
            alarmSeverity = tbNotification.info.alarmSeverity,
            alarmStatus = tbNotification.info.alarmStatus,
            dashboardId = tbNotification.info.dashboardId,
            status = tbNotification.status
        )
    }

    fun markAsRead(
        notificationId: Id,
        thingsBoardUserFromApi: ThingsBoardUser
    ): Boolean {
        logger.info("markAsRead: id: $notificationId user: $thingsBoardUserFromApi")
val url = "$thingsBoardApiUrl/api/notification/${notificationId.id}/read"
        val requestEntity = HttpEntity<String>(tbServiceUtil.getAuthHeaders(thingsBoardUserFromApi.id.id))
        try {
            val response =
                restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Any::class.java)
            return response.statusCode == HttpStatus.OK
        } catch (e: HttpClientErrorException) {
            logger.error("Error markAsRead:$e")
            return false
        }
    }
}