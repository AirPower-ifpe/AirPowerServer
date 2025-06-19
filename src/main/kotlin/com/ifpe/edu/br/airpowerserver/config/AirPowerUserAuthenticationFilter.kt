package com.ifpe.edu.br.airpowerserver.config

import com.ifpe.edu.br.airpowerserver.dto.error.ErrorCode
import com.ifpe.edu.br.airpowerserver.repository.airpower.AirPowerUserRepository
import com.ifpe.edu.br.airpowerserver.service.AirPowerTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AirPowerUserAuthenticationFilter(
    private val tokenService: AirPowerTokenService,
    private val airpowerUserRepository: AirPowerUserRepository,
    private val errorResponseWriter: ErrorResponseWriter
) : OncePerRequestFilter() {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!isEndpointPrivate(request)) {
            filterChain.doFilter(request, response)
            return
        }
        try {
            log.info("doFilterInternal on private endpoint: {}", request.requestURI)
            val jwt = parseJwtFromRequest(request)
            val subject = tokenService.getSubjectFromToken(jwt)
            val user = airpowerUserRepository.findByEmail(subject)
            val userDetails = AirPowerUserDetailsImpl(user)
            val authentication = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            log.warn("Falha na autenticação do token JWT: {}", e.message)
            SecurityContextHolder.clearContext()
            errorResponseWriter.writeErrorResponse(response, ErrorCode.INVALID_AIRPOWER_TOKEN)
            return
        }
    }


    private fun isEndpointPrivate(request: HttpServletRequest): Boolean {
        return !AirPowerSecurityConfiguration.PUBLIC_ENDPOINTS.contains(request.requestURI)
    }

    private fun parseJwtFromRequest(
        request: HttpServletRequest
    ): String {
        val headerAuth = request.getHeader("Authorization")
        if (!headerAuth.isNullOrBlank() && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7)
        } else {
            throw IllegalStateException("Invalid Authorization header")
        }
    }
}