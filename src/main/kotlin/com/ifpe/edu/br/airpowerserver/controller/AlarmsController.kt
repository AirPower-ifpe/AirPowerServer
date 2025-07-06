package com.ifpe.edu.br.airpowerserver.controller

import com.ifpe.edu.br.airpowerserver.config.DownstreamServiceException
import com.ifpe.edu.br.airpowerserver.dto.alarms.APAlarmInfo
import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardAlarmService
import com.ifpe.edu.br.airpowerserver.service.ThingsBoardUserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/alarms")
class AlarmsController(
    private val airPowerUserRepository: AirPowerUserRepository,
    private val thingsBoardService: ThingsBoardUserService,
    private val alarmService: ThingsBoardAlarmService
) {
    private val logger = LoggerFactory.getLogger(AlarmsController::class.java)

    @GetMapping("/me")
    fun getAlarmsForCurrentUserId(): ResponseEntity<Any> {
        logger.info("getAlarmsForCurrentUserId()")
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
            ResponseEntity.ok(alarmService.getAllAlarmInfoForUserId(thingsBoardUserFromApi.id.id))
        }
    }
}