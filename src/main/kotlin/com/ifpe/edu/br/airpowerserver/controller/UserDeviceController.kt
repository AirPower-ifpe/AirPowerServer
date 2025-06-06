package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.dto.DeviceSummaryDTO
import com.ifpe.edu.br.airpowerserver.service.UserDeviceService
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

    @GetMapping("/{userId}/devices-summary")
    fun getDeviceSummariesForUser(@PathVariable userId: String): ResponseEntity<List<DeviceSummaryDTO>> {
        val deviceSummaries = userDeviceService.getDeviceSummariesForUser(userId)
        return if (deviceSummaries.isNotEmpty()) {
            ResponseEntity.ok(deviceSummaries)
        } else {
            ResponseEntity.ok(emptyList())
        }
    }
}