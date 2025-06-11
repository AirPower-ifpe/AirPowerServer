package com.ifpe.edu.br.airpowerserver.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.net.HttpURLConnection
import javax.net.ssl.*

@Configuration
class AppSSLConfig {

    private val logger = LoggerFactory.getLogger(AppSSLConfig::class.java)

    @Bean
    fun restTemplate(): RestTemplate {
        // Cria um TrustManager que não valida cadeias de certificados (confia em todos)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
            override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val requestFactory = object : SimpleClientHttpRequestFactory() {
            override fun prepareConnection(connection: HttpURLConnection, httpMethod: String) {
                if (connection is HttpsURLConnection) {
                    connection.sslSocketFactory = sslContext.socketFactory
                    connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
                super.prepareConnection(connection, httpMethod)
            }
        }
        logger.error("ATTENTION: THIS SOFTWARE IGNORES CERTIFICATION CHECK, IT MUST NOT BE PUT IN PRODUCTION AS IS")
        return RestTemplate(requestFactory)
    }
}