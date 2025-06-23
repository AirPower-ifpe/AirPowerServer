package com.ifpe.edu.br.airpowerserver.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.ifpe.edu.br.airpowerserver.repository.airpower"],
    entityManagerFactoryRef = "airpowerEntityManagerFactory",
    transactionManagerRef = "airpowerTransactionManager"
)
class AirPowerJpaConfig {

    @Primary
    @Bean(name = ["airpowerDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource.airpower")
    fun airpowerDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Primary
    @Bean(name = ["airpowerEntityManagerFactory"])
    fun airpowerEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("airpowerDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val jpaProperties = mapOf(
            "hibernate.hbm2ddl.auto" to "create",
            "hibernate.show_sql" to "false",
            "hibernate.format_sql" to "true",
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect"
        )

        return builder
            .dataSource(dataSource)
            .packages("com.ifpe.edu.br.airpowerserver.entity.airpower")
            .persistenceUnit("airpower")
            .properties(jpaProperties)
            .build()
    }

    @Primary
    @Bean(name = ["airpowerTransactionManager"])
    fun airpowerTransactionManager(
        @Qualifier("airpowerEntityManagerFactory") emf: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(emf)
    }
}