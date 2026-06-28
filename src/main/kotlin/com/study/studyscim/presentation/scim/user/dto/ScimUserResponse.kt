package com.study.studyscim.presentation.scim.user.dto

import com.study.studyscim.presentation.scim.shared.ScimEmail
import com.study.studyscim.presentation.scim.shared.ScimMeta
import com.study.studyscim.presentation.scim.shared.ScimName

data class ScimUserResponse(
    val schemas: List<String> = listOf("urn:ietf:params:scim:schemas:core:2.0:User"),
    val id: String,
    val externalId: String? = null,
    val userName: String,
    val name: ScimName? = null,
    val displayName: String? = null,
    val emails: List<ScimEmail>? = null,
    val active: Boolean,
    val meta: ScimMeta,
)