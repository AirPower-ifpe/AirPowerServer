package com.ifpe.edu.br.airpowerserver.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test/api/v1")
class TestController {

    @GetMapping("/test")
    fun getPublicData(): ResponseEntity<Map<String, String>> {
        val data = mapOf(
            "message" to "this is a public data from AirPowerServer.",
            "timestamp" to java.time.LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(data)
    }
}