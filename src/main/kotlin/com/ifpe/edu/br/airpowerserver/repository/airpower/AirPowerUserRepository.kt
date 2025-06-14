package com.ifpe.edu.br.airpowerserver.repository.airpower

import com.ifpe.edu.br.airpowerserver.entity.airpower.AirPowerUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AirPowerUserRepository : JpaRepository<AirPowerUser, UUID> {
    fun findByEmail(email: String): AirPowerUser
}