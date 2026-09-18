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

/**
 * Service responsible for managing user notifications by interacting with the ThingsBoard API.
 *
 * This class handles fetching notifications for a specific user and marking them as read.
 * It communicates with the ThingsBoard external service to perform these operations.
 *
 * @property restTemplate The Spring RestTemplate for making HTTP requests.
 * @property tbServiceUtil A utility service for handling common ThingsBoard API interactions, such as authentication.
 */
@Service
class NotificationService(
    private val restTemplate: RestTemplate,
    private val tbServiceUtil: ThingsBoardServiceUtil
) {
    @Value("\${thingsboard.api.url}")
    private lateinit var thingsBoardApiUrl: String

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Retrieves notifications for a user from the ThingsBoard API.
     *
     * This method fetches the 50 most recent unread notifications for the specified user.
     * The results are sorted by creation time in descending order (newest first).
     * Each notification is then transformed into a standardized [AirPowerNotificationItem].
     *
     * @param userId The UUID of the user whose notifications are to be fetched.
     * @return A list of [AirPowerNotificationItem] representing the user's unread notifications.
     *         Returns an empty list if an error occurs or no notifications are found.
     */
    fun getUnreadNotificationsForUser(userId: UUID): List<AirPowerNotificationItem> {
        logger.info("Fetching notifications from ThingsBoard for user: $userId")
        val url =
            "$thingsBoardApiUrl/api/notifications?page=0&pageSize=50&sortProperty=createdTime&sortOrder=DESC&unreadOnly=true"
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
     * Transforms a [ThingsBoardNotification] object into a standardized [AirPowerNotificationItem].
     *
     * This private helper method maps the fields from the source object, including the nested
     * `info` object, to the flattened structure of [AirPowerNotificationItem].
     *
     * @param tbNotification The source [ThingsBoardNotification] object from the ThingsBoard API.
     * @return An [AirPowerNotificationItem] containing the standardized notification data.
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

    /**
     * Marks a specific notification as read for a user.
     *
     * This method sends a PUT request to the ThingsBoard API to update the status of a
     * notification to 'READ'.
     *
     * @param notificationId The [Id] of the notification to be marked as read.
     * @param thingsBoardUserFromApi The [ThingsBoardUser] object, used to get authentication credentials.
     * @return `true` if the notification was successfully marked as read (HTTP 200 OK), `false` otherwise.
     */
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