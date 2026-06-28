package com.study.studyscim.application.user

import com.study.studyscim.domain.user.User
import com.study.studyscim.domain.user.UserRepository
import com.study.studyscim.presentation.scim.shared.ScimListResponse
import com.unboundid.scim2.common.filters.Filter
import com.unboundid.scim2.common.filters.FilterType
import com.unboundid.scim2.common.messages.PatchRequest
import com.unboundid.scim2.common.types.Email
import com.unboundid.scim2.common.types.Meta
import com.unboundid.scim2.common.types.Name
import com.unboundid.scim2.common.types.UserResource
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    @Value($$"${scim.base-url}") private val baseUrl: String,
) {

    fun listUsers(filter: String?, startIndex: Int, count: Int): ScimListResponse<UserResource> {
        val users = if (filter != null) parseAndApplyFilter(filter) else userRepository.findAll()
        val page = users.drop(startIndex - 1).take(count)
        return ScimListResponse(
            totalResults = users.size,
            startIndex = startIndex,
            itemsPerPage = page.size,
            Resources = page.map { toScimResource(it) },
        )
    }

    @Transactional
    fun createUser(resource: UserResource): UserResource =
        toScimResource(userRepository.save(User.from(resource)))

    fun getUser(id: UUID): UserResource {
        val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User $id not found")
        return toScimResource(user)
    }

    @Transactional
    fun replaceUser(id: UUID, resource: UserResource): UserResource {
        val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User $id not found")
        user.applyReplace(resource)
        return toScimResource(userRepository.save(user))
    }

    @Transactional
    fun patchUser(id: UUID, patchRequest: PatchRequest): UserResource {
        val user = userRepository.findByIdOrNull(id) ?: throw UserNotFoundException("User $id not found")
        val updated = patchRequest.applyToResource(toScimResource(user))
        user.applyPatch(updated)
        return toScimResource(userRepository.save(user))
    }

    @Transactional
    fun deleteUser(id: UUID) {
        if (!userRepository.existsById(id)) throw UserNotFoundException("User $id not found")
        userRepository.deleteById(id)
    }

    private fun parseAndApplyFilter(filter: String): List<User> {
        return try {
            applyScimFilter(Filter.fromString(filter))
        } catch (_: Exception) {
            userRepository.findAll()
        }
    }

    private fun applyScimFilter(filter: Filter): List<User> {
        if (filter.filterType != FilterType.EQUAL) return userRepository.findAll()
        val attrName = filter.attributePath?.toString()?.lowercase() ?: return userRepository.findAll()
        val value = filter.comparisonValue?.asString() ?: return userRepository.findAll()
        return when (attrName) {
            "username" -> listOfNotNull(userRepository.findByUserName(value))
            "externalid" -> listOfNotNull(userRepository.findByExternalId(value))
            else -> userRepository.findAll()
        }
    }

    fun toScimResource(user: User): UserResource {
        val resource = UserResource()
        resource.id = user.id.toString()
        resource.externalId = user.externalId
        resource.userName = user.userName

        if (user.givenName != null || user.familyName != null) {
            resource.setName(
                Name()
                    .setFormatted(listOfNotNull(user.givenName, user.familyName).joinToString(" "))
                    .setGivenName(user.givenName)
                    .setFamilyName(user.familyName),
            )
        }

        user.displayName?.let { resource.setDisplayName(it) }
        resource.setActive(user.active)

        user.email?.let {
            resource.setEmails(Email().setValue(it).setType("work").setPrimary(true))
        }

        resource.meta = Meta()
            .setResourceType("User")
            .setCreatedMillis(user.createdAt.toEpochMilli())
            .setLastModifiedMillis(user.updatedAt.toEpochMilli())
            .setLocation(URI.create("$baseUrl/scim/v2/Users/${user.id}"))
            .setVersion("W/\"${user.updatedAt.toEpochMilli()}\"")

        return resource
    }
}