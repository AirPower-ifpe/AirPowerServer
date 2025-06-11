package com.ifpe.edu.br.airpowerserver.repository.airpower

import com.ifpe.edu.br.airpowerserver.entity.airpower.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuthRepository : JpaRepository<RefreshToken, UUID> {

    fun findByToken(token: String): RefreshToken
}