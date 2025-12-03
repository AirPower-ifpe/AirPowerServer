package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardInfo
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardDashboardService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/user/{userId}/dashboards")
class DashboardController(
    private val dashboardService: ThingsBoardDashboardService
) {
    private val logger = LoggerFactory.getLogger(DashboardController::class.java)

    @GetMapping
    fun getDashboardsForUser(@PathVariable userId: String): ResponseEntity<List<DashboardInfo>> {
        logger.info("Request received for dashboards of user: {}", userId)
        val dashboards = dashboardService.getDashboardsForUser(UUID.fromString(userId))
        return ResponseEntity.ok(dashboards)
    }

    @GetMapping("/{dashboardId}/device-ids")
    fun getDeviceIdsFromDashboard(
        @PathVariable userId: String,
        @PathVariable dashboardId: String
    ): ResponseEntity<List<String>> {
        logger.info("Request received for device IDs from dashboard {} for user: {}", dashboardId, userId)
        val idObjects = dashboardService.getDeviceIdsFromDashboard(UUID.fromString(userId), dashboardId)
        val uuidStrings = idObjects.map { it.id.toString() }
        return ResponseEntity.ok(uuidStrings)
    }
}