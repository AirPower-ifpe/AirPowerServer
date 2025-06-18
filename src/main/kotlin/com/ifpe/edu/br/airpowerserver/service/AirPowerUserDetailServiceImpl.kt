package com.ifpe.edu.br.airpowerserver.service

import com.ifpe.edu.br.airpowerserver.config.AirPowerUserDetailsImpl
import com.ifpe.edu.br.airpowerserver.entity.airpower.AirPowerUser
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class AirPowerUserDetailServiceImpl(
    val airPowerUserRepository: AirPowerUserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails? {
        val airPowerUser: AirPowerUser? =
            airPowerUserRepository.findByEmail(username)
        if (airPowerUser == null) {
            throw RuntimeException("User with email $username not found")
        }
        return AirPowerUserDetailsImpl(airPowerUser)
    }
}