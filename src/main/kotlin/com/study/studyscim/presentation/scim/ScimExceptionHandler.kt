package com.study.studyscim.presentation.scim

import com.study.studyscim.application.user.UserNotFoundException
import com.study.studyscim.presentation.scim.shared.ScimError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.study.studyscim.presentation.scim"])
class ScimExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleNotFound(ex: UserNotFoundException): ResponseEntity<ScimError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ScimError(status = "404", scimType = "noTarget", detail = ex.message ?: "Not found"),
        )

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ScimError> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ScimError(status = "500", detail = ex.message ?: "Internal server error"),
        )
}