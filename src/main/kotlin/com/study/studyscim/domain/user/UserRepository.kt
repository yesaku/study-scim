package com.study.studyscim.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUserName(userName: String): User?
    fun findByExternalId(externalId: String): User?
}
