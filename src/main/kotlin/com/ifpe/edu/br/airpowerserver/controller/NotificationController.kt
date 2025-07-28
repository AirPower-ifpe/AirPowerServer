package com.ifpe.edu.br.airpowerserver.controller


import com.ifpe.edu.br.airpowerserver.config.DownstreamServiceException
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.dto.notification.AirPowerNotificationItem
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.service.NotificationService
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val airPowerUserRepository: AirPowerUserRepository,
    private val thingsBoardService: ThingsBoardUserService,
) {

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
            val notifications = notificationService.getNotificationsForUser(thingsBoardUserFromApi.id.id)
            ResponseEntity.ok(notifications)
        }
    }
}