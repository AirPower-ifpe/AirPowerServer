package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.config.ApiException
import com.ifpe.edu.br.airpowerserver.dto.dashboards.DashboardInfo
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardDashboardService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * Controller responsible for handling requests related to dashboards.
 *
 * This controller provides endpoints for retrieving dashboard information and
 * associated device IDs.
 *
 * @property dashboardService The service responsible for dashboard-related business logic.
 */
@RestController
@RequestMapping("/api/v1/dashboards/")
class DashboardController(
    private val dashboardService: ThingsBoardDashboardService
) {
    private val logger = LoggerFactory.getLogger(DashboardController::class.java)

    /**
     * Retrieves all dashboards associated with a specific user.
     *
     * @param userId The UUID String of the user.
     * @return A [ResponseEntity] containing a list of [DashboardInfo] objects.
     */
    @GetMapping("{userId}/dashboards")
    fun getDashboardsForUser(@PathVariable userId: String): ResponseEntity<List<DashboardInfo>> {
        logger.info("Request received for dashboards of user: {}", userId)
        runCatching { UUID.fromString(userId) }.getOrElse {
            logger.error("Invalid userId format: {}", userId)
            throw ApiException(ErrorCode.BAD_REQUEST,"Invalid userId format" )
        }
        val dashboards = dashboardService.getDashboardsForUser(UUID.fromString(userId))
        return ResponseEntity.ok(dashboards)
    }

    /**
     * Retrieves all device IDs associated with a specific dashboard.
     *
     * @param dashboardId The UUID String of the dashboard.
     * @return A [ResponseEntity] containing a list of device ID strings.
     */
    @GetMapping("{dashboardId}/device-ids")
    fun getDeviceIdsFromDashboard(
        @PathVariable dashboardId: String
    ): ResponseEntity<List<String>> {
        logger.info("Request received for device IDs from dashboard {}", dashboardId)
        val idObjects = dashboardService.getDeviceIdsFromDashboard(dashboardId)
        val uuidStrings = idObjects.map { it.id.toString() }
        return ResponseEntity.ok(uuidStrings)
    }
}