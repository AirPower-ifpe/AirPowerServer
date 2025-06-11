package com.ifpe.edu.br.airpowerserver.entity.airpower

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    var token: String,

    @Column(nullable = false)
    var userId: String,

    @Column(nullable = false)
    var expiryDate: Instant
)