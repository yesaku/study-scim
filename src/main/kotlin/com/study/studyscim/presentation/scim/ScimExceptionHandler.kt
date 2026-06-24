package com.study.studyscim.presentation.scim

import com.study.studyscim.application.user.ScimNotFoundException
import com.study.studyscim.presentation.scim.dto.ScimError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ScimExceptionHandler {

    @ExceptionHandler(ScimNotFoundException::class)
    fun handleNotFound(ex: ScimNotFoundException): ResponseEntity<ScimError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ScimError(status = "404", scimType = "noTarget", detail = ex.message ?: "Not found"),
        )

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ScimError> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ScimError(status = "500", detail = ex.message ?: "Internal server error"),
        )
}
