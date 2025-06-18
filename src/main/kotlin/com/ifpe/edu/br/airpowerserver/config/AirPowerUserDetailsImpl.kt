package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.entity.airpower.AirPowerUser
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails


class AirPowerUserDetailsImpl(
    val airPowerUser: AirPowerUser
) : UserDetails {

    private val logger = LoggerFactory.getLogger(AirPowerUserDetailsImpl::class.java)

    override fun getAuthorities(): Collection<GrantedAuthority?>? {
        val role = airPowerUser.role ?: return emptyList()
        val authority = SimpleGrantedAuthority("${role.name}")
        return listOf(authority)
    }

    override fun getPassword(): String? {
        return airPowerUser.password
    }

    override fun getUsername(): String? {
        return airPowerUser.email
    }
}