package com.ifpe.edu.br.airpowerserver.config

import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Configuration
class AppSSLConfig {

    private val logger = LoggerFactory.getLogger(AppSSLConfig::class.java)

    @Bean
    fun restTemplate(errorHandler: ServerResponseErrorHandler): RestTemplate {
        // Cria um TrustManager que não valida cadeias de certificados (confia em todos)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
            override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .build()
        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build()
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = httpClient
        logger.error("ATTENTION: THIS SOFTWARE IGNORES CERTIFICATION CHECK, IT MUST NOT BE PUT IN PRODUCTION AS IS")
        val restTemplate = RestTemplate(requestFactory)
        restTemplate.errorHandler = errorHandler
        return restTemplate
    }
}