package com.study.studyscim.domain.user

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
)
