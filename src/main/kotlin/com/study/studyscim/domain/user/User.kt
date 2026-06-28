package com.study.studyscim.domain.user

import com.unboundid.scim2.common.types.UserResource
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true)
    var externalId: String? = null,

    @Column(nullable = false, unique = true, length = 255)
    var userName: String,

    var givenName: String? = null,
    var familyName: String? = null,
    var displayName: String? = null,

    @Column(name = "primary_email")
    var email: String? = null,

    var active: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    companion object {
        fun from(resource: UserResource): User = User(
            externalId = resource.externalId,
            userName = resource.userName ?: throw IllegalArgumentException("userName is required"),
            givenName = resource.name?.givenName,
            familyName = resource.name?.familyName,
            displayName = resource.displayName ?: resource.buildDisplayName(),
            email = resource.primaryEmail(),
            active = resource.active ?: true,
        )
    }

    fun applyReplace(resource: UserResource) {
        externalId = resource.externalId
        userName = resource.userName ?: userName
        givenName = resource.name?.givenName
        familyName = resource.name?.familyName
        displayName = resource.displayName ?: resource.buildDisplayName()
        email = resource.primaryEmail()
        active = resource.active ?: true
        updatedAt = Instant.now()
    }

    fun applyPatch(updated: UserResource) {
        externalId = updated.externalId
        updated.userName?.let { userName = it }
        givenName = updated.name?.givenName
        familyName = updated.name?.familyName
        displayName = updated.displayName
        email = updated.primaryEmail()
        active = updated.active ?: active
        updatedAt = Instant.now()
    }
}

private fun UserResource.primaryEmail(): String? =
    emails?.firstOrNull { it.primary == true }?.value
        ?: emails?.firstOrNull()?.value

private fun UserResource.buildDisplayName(): String? =
    listOfNotNull(name?.givenName, name?.familyName).joinToString(" ").ifBlank { null }