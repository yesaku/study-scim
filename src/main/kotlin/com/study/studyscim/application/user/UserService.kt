package com.study.studyscim.application.user

import com.study.studyscim.domain.user.User
import com.study.studyscim.domain.user.UserRepository
import com.study.studyscim.presentation.scim.shared.*
import com.study.studyscim.presentation.scim.user.dto.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    @Value("\${scim.base-url}") private val baseUrl: String,
) {

    fun listUsers(filter: String?, startIndex: Int, count: Int): ScimListResponse<ScimUserResponse> {
        val users = if (filter != null) parseAndApplyFilter(filter) else userRepository.findAll()
        val page = users.drop(startIndex - 1).take(count)
        return ScimListResponse(
            totalResults = users.size,
            startIndex = startIndex,
            itemsPerPage = page.size,
            Resources = page.map { toScimResponse(it) },
        )
    }

    @Transactional
    fun createUser(request: ScimUserRequest): ScimUserResponse {
        val user = User(
            externalId = request.externalId,
            userName = request.userName,
            givenName = request.name?.givenName,
            familyName = request.name?.familyName,
            displayName = request.displayName ?: buildDisplayName(request.name),
            email = request.primaryEmail(),
            active = request.active,
        )
        return toScimResponse(userRepository.save(user))
    }

    fun getUser(id: UUID): ScimUserResponse {
        val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User $id not found")
        return toScimResponse(user)
    }

    @Transactional
    fun replaceUser(id: UUID, request: ScimUserRequest): ScimUserResponse {
        val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User $id not found")
        user.externalId = request.externalId
        user.userName = request.userName
        user.givenName = request.name?.givenName
        user.familyName = request.name?.familyName
        user.displayName = request.displayName ?: buildDisplayName(request.name)
        user.email = request.primaryEmail()
        user.active = request.active
        user.updatedAt = Instant.now()
        return toScimResponse(userRepository.save(user))
    }

    @Transactional
    fun patchUser(id: UUID, request: ScimPatchRequest): ScimUserResponse {
        val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User $id not found")
        request.Operations.forEach { applyPatchOperation(user, it) }
        user.updatedAt = Instant.now()
        return toScimResponse(userRepository.save(user))
    }

    @Transactional
    fun deleteUser(id: UUID) {
        if (!userRepository.existsById(id)) throw UserNotFoundException("User $id not found")
        userRepository.deleteById(id)
    }

    private fun parseAndApplyFilter(filter: String): List<User> {
        val match = Regex("""(\w+)\s+eq\s+"([^"]+)"""").find(filter) ?: return userRepository.findAll()
        return when (match.groupValues[1].lowercase()) {
            "username" -> listOfNotNull(userRepository.findByUserName(match.groupValues[2]))
            "externalid" -> listOfNotNull(userRepository.findByExternalId(match.groupValues[2]))
            else -> userRepository.findAll()
        }
    }

    private fun applyPatchOperation(user: User, op: ScimPatchOperation) {
        val path = op.path?.lowercase() ?: return
        val value = op.value?.toString()
        when (op.op.lowercase()) {
            "replace", "add" -> when {
                path == "active" -> user.active = value?.lowercase() != "false"
                path == "username" -> value?.let { user.userName = it }
                path == "displayname" -> user.displayName = value
                path == "name.givenname" -> user.givenName = value
                path == "name.familyname" -> user.familyName = value
                path == "externalid" -> user.externalId = value
                path.contains("emails") -> user.email = value
            }
            "remove" -> when (path) {
                "active" -> user.active = false
                "displayname" -> user.displayName = null
            }
        }
    }

    private fun buildDisplayName(name: ScimName?): String? =
        listOfNotNull(name?.givenName, name?.familyName).joinToString(" ").ifBlank { null }

    private fun ScimUserRequest.primaryEmail(): String? =
        emails?.firstOrNull { it.primary }?.value ?: emails?.firstOrNull()?.value

    fun toScimResponse(user: User): ScimUserResponse {
        val formatter = DateTimeFormatter.ISO_INSTANT
        val name = if (user.givenName != null || user.familyName != null) {
            ScimName(
                formatted = listOfNotNull(user.givenName, user.familyName).joinToString(" "),
                givenName = user.givenName,
                familyName = user.familyName,
            )
        } else null

        return ScimUserResponse(
            id = user.id.toString(),
            externalId = user.externalId,
            userName = user.userName,
            name = name,
            displayName = user.displayName,
            emails = user.email?.let { listOf(ScimEmail(value = it)) },
            active = user.active,
            meta = ScimMeta(
                resourceType = "User",
                created = formatter.format(user.createdAt),
                lastModified = formatter.format(user.updatedAt),
                location = "$baseUrl/scim/v2/Users/${user.id}",
                version = "W/\"${user.updatedAt.toEpochMilli()}\"",
            ),
        )
    }
}