package com.ifpe.edu.br.airpowerserver.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey
import java.util.Base64

@Component
class JwtUtils {

    @Value("\${thingsboard.jwt.secret.base64}") // Nome da propriedade para a chave Base64 encoded
    private lateinit var jwtSecretBase64Encoded: String

    private val jwtSecret: SecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)
    private val jwtExpirationMs = 86400000 // 1 dia

    fun generateJwtToken(username: String): String {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(jwtSecret)
            .compact()
    }

    private val signingKey: SecretKey by lazy {
        val decodedKeyBytes = Base64.getDecoder().decode(jwtSecretBase64Encoded)
        Keys.hmacShaKeyFor(decodedKeyBytes)
    }

    fun getUserNameFromJwtToken(token: String): String {
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build()
            .parseClaimsJws(token).body.subject
    }

    fun validateJwtToken(authToken: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(authToken)
            true
        } catch (e: Exception) {
            false
        }
    }
}