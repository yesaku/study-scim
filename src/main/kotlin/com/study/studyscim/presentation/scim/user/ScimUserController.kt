package com.study.studyscim.presentation.scim.user

import com.study.studyscim.application.user.UserService
import com.study.studyscim.presentation.scim.shared.ScimListResponse
import com.study.studyscim.presentation.scim.user.dto.ScimPatchRequest
import com.study.studyscim.presentation.scim.user.dto.ScimUserRequest
import com.study.studyscim.presentation.scim.user.dto.ScimUserResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/scim/v2/Users")
class ScimUserController(private val userService: UserService) {

    @GetMapping(produces = ["application/scim+json", "application/json"])
    fun listUsers(
        @RequestParam(required = false) filter: String?,
        @RequestParam(defaultValue = "1") startIndex: Int,
        @RequestParam(defaultValue = "100") count: Int,
    ): ScimListResponse<ScimUserResponse> = userService.listUsers(filter, startIndex, count)

    @PostMapping(
        consumes = ["application/scim+json", "application/json"],
        produces = ["application/scim+json", "application/json"],
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody request: ScimUserRequest): ScimUserResponse =
        userService.createUser(request)

    @GetMapping("/{id}", produces = ["application/scim+json", "application/json"])
    fun getUser(@PathVariable id: UUID): ScimUserResponse =
        userService.getUser(id)

    @PutMapping(
        "/{id}",
        consumes = ["application/scim+json", "application/json"],
        produces = ["application/scim+json", "application/json"],
    )
    fun replaceUser(@PathVariable id: UUID, @RequestBody request: ScimUserRequest): ScimUserResponse =
        userService.replaceUser(id, request)

    @PatchMapping(
        "/{id}",
        consumes = ["application/scim+json", "application/json"],
        produces = ["application/scim+json", "application/json"],
    )
    fun patchUser(@PathVariable id: UUID, @RequestBody request: ScimPatchRequest): ScimUserResponse =
        userService.patchUser(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: UUID) = userService.deleteUser(id)
}