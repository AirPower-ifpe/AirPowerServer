package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.DeviceSummaryDTO
import com.ifpe.edu.br.airpowerserver.service.DeviceTelemetryService
import com.ifpe.edu.br.airpowerserver.service.UserDeviceService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test/api/v1/user")
class UserDeviceController(
    private val userDeviceService: UserDeviceService
) {
    private val logger = LoggerFactory.getLogger(DeviceTelemetryService::class.java)
    @GetMapping("/{userId}/devices-summary")
    fun getDeviceSummariesForUser(@PathVariable userId: String): ResponseEntity<List<DeviceSummaryDTO>> {
        logger.warn("Received getDeviceSummariesForUser user id: $userId")
        val deviceSummaries = userDeviceService.getDeviceSummariesForUser(userId)
        return ResponseEntity.ok(deviceSummaries)
    }
}