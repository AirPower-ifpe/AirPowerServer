package com.ifpe.edu.br.airpowerserver.entity.airpower

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "persist_token")
class PersistToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    var jwt: String,

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    var refreshToken: String,

    @Column(nullable = false)
    var userId: UUID,

    @Column(nullable = false)
    var scope: Int
)