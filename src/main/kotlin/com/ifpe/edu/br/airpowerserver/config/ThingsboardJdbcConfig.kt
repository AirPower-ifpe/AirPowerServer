package com.ifpe.edu.br.airpowerserver.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
class ThingsboardJdbcConfig {

    @Bean(name = ["thingsboardDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource.thingsboard")
    fun thingsboardDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean
    fun jdbcTemplate(@Qualifier("thingsboardDataSource") dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }
}