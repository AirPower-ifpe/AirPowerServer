package com.ifpe.edu.br.airpowerserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class AirPowerServerApplication

fun main(args: Array<String>) {
    runApplication<AirPowerServerApplication>(*args)
}