package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.service.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class AuthTokenFilter(
    private val tokenService: TokenService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = parseJwt(request)
            if (jwt != null && tokenService.validateJwtToken(jwt)) {
//                val userId = tokenService.validateAndGetUserIdFromRefreshToken(jwt)
//                val userDetails = userDetailsService.loadUserByUsername(userId)
//                val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
//                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
//                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            log.error("Cannot set user authentication", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun parseJwt(request: HttpServletRequest): String? {
        val headerAuth = request.getHeader("Authorization")
        return if (!headerAuth.isNullOrBlank() && headerAuth.startsWith("Bearer ")) {
            headerAuth.substring(7)
        } else null
    }
}