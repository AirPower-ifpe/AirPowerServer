package com.ifpe.edu.br.airpowerserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

//exclude = [DataSourceAutoConfiguration::class]
@SpringBootApplication()

class AirPowerServerApplication

fun main(args: Array<String>) {
    runApplication<AirPowerServerApplication>(*args)
}