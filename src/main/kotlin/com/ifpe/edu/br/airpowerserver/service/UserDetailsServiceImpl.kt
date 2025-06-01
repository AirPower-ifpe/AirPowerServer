package com.ifpe.edu.br.airpowerserver.service

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    private val users = mapOf(
        "user" to User.builder()
            .username("user")
            .password(passwordEncoder.encode("user123"))
            .roles("USER")
            .build(),
        "admin" to User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN")
            .build()
    )

    override fun loadUserByUsername(username: String): UserDetails {
        return users[username] ?: throw UsernameNotFoundException("User not found: $username")
    }
}