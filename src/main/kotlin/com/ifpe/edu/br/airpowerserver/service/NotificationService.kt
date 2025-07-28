package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.dto.notification.NotificationItem
import com.ifpe.edu.br.airpowerserver.dto.notification.TbNotificationDto
import com.ifpe.edu.br.airpowerserver.dto.notification.ThingsBoardNotificationResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
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
     * @return Uma lista de [NotificationItem] ordenada da mais recente para a mais antiga.
     */
    fun getNotificationsForUser(userId: UUID): List<NotificationItem> {
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
     * @param tbNotification O [TbNotificationDto] a ser transformado.
     * @return Um objeto [NotificationItem].
     */
    private fun transformTbNotificationToNotificationItem(
        tbNotification: TbNotificationDto
    ): NotificationItem {
        return NotificationItem(
            label = tbNotification.subject,
            message = tbNotification.text,
            timestamp = tbNotification.createdTime,
            isNew = tbNotification.status == "UNREAD"
        )
    }
}