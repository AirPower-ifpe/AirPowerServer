package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.controller.AuthController
import com.ifpe.edu.br.airpowerserver.entity.airpower.AirPowerUser
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.stream.Collectors


class AirPowerUserDetailsImpl(
    val airPowerUser: AirPowerUser
) : UserDetails {

    private val logger = LoggerFactory.getLogger(AirPowerUserDetailsImpl::class.java)

    override fun getAuthorities(): Collection<GrantedAuthority?>? {
        logger.error("getAuthorities(): $airPowerUser") // todo apagar
        return airPowerUser.roles
            ?.stream()
            ?.map { role -> SimpleGrantedAuthority(role?.name?.name) }
            ?.collect(Collectors.toList())
    }

    override fun getPassword(): String? {
        return airPowerUser.password
    }

    override fun getUsername(): String? {
        return airPowerUser.email
    }
}