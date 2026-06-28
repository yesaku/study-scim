package com.study.studyscim.presentation.scim.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class BearerTokenAuthFilter(
    @Value("\${scim.bearer-token}") private val expectedToken: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        if (token != expectedToken) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/scim+json"
            response.writer.write(
                """{"schemas":["urn:ietf:params:scim:api:messages:2.0:Error"],"status":"401","detail":"Unauthorized"}"""
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/actuator")
}