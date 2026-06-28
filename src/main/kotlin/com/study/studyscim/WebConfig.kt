package com.study.studyscim

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class WebConfig {

    @Bean
    fun requestLoggingFilter(): ScimRequestLoggingFilter =
        ScimRequestLoggingFilter().apply {
            setIncludeQueryString(true)
            setIncludePayload(true)
            setMaxPayloadLength(2000)
            setIncludeHeaders(false)
        }
}

class ScimRequestLoggingFilter : CommonsRequestLoggingFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/actuator")
}
