package com.ifpe.edu.br.airpowerserver.controller


import com.ifpe.edu.br.airpowerserver.config.DownstreamServiceException
import com.ifpe.edu.br.airpowerserver.dto.Id
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.dto.notification.AirPowerNotificationItem
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.service.NotificationService
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for managing user notifications.
 *
 * This controller provides endpoints for authenticated users to retrieve their notifications
 * and to mark specific notifications as read. It interacts with [NotificationService]
 * and [ThingsBoardUserService] to fetch and update notification data.
 *
 * @property notificationService The service responsible for notification-related business logic.
 * @property airPowerUserRepository Repository for accessing AirPower user data.
 * @property thingsBoardService Service for interacting with ThingsBoard user-related functionalities.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val airPowerUserRepository: AirPowerUserRepository,
    private val thingsBoardService: ThingsBoardUserService,
) {

    /**
     * Retrieves all unread notifications for the currently authenticated user.
     *
     * The user's identity is extracted from the security context. If the user cannot be
     * identified, a [DownstreamServiceException] is thrown.
     *
     * @return A [ResponseEntity] containing a list of [AirPowerNotificationItem] for the user.
     * @throws DownstreamServiceException if the authenticated user's information cannot be found.
     */
    @GetMapping("/me")
    fun getMyNotifications(): ResponseEntity<List<AirPowerNotificationItem>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication?.name
        return if (username == null) {
            throw DownstreamServiceException(
                ErrorCode.INVALID_REFRESH_TOKEN,
                "user could not be found"
            )
        } else {
            val storedAirPowerUser = airPowerUserRepository.findByEmail(username)
            val thingsBoardUserFromApi = thingsBoardService.getCurrentUser(storedAirPowerUser.id!!)
            val notifications = notificationService.getUnreadNotificationsForUser(thingsBoardUserFromApi.id.id)
            ResponseEntity.ok(notifications)
        }
    }

    /**
     * Marks a specific notification as read for the currently authenticated user.
     *
     * The notification to be marked is identified by its [Id] provided in the request body.
     * The user's identity is extracted from the security context. If the user cannot be
     * identified, a [DownstreamServiceException] is thrown.
     *
     * @param id The [Id] object containing the UUID of the notification to mark as read.
     * @return A [ResponseEntity] containing `true` if the notification was successfully
     *         marked as read, `false` otherwise.
     * @throws DownstreamServiceException if the authenticated user's information cannot be found.
     */
    @PostMapping("/read")
    fun refresh(
        @RequestBody id: Id
    ): ResponseEntity<Boolean> {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication?.name
        return if (username == null) {
            throw DownstreamServiceException(
                ErrorCode.INVALID_AIRPOWER_TOKEN,
                "user could not be found"
            )
        } else {
            val storedAirPowerUser = airPowerUserRepository.findByEmail(username)
            val thingsBoardUserFromApi = thingsBoardService.getCurrentUser(storedAirPowerUser.id!!)
            val result = notificationService.markAsRead(
                Id(
                    id = id.id,
                    entityType = ""
                ), thingsBoardUserFromApi
            )
            ResponseEntity.ok(result)
        }
    }
}