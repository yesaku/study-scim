package com.study.studyscim.presentation.scim.user.dto

import com.study.studyscim.presentation.scim.shared.ScimEmail
import com.study.studyscim.presentation.scim.shared.ScimName

data class ScimUserRequest(
    val schemas: List<String>? = null,
    val externalId: String? = null,
    val userName: String,
    val name: ScimName? = null,
    val displayName: String? = null,
    val emails: List<ScimEmail>? = null,
    val active: Boolean = true,
)

data class ScimPatchOperation(
    val op: String,
    val path: String? = null,
    val value: Any? = null,
)

// Operations は SCIM RFC 7644 仕様で大文字始まりが必須
data class ScimPatchRequest(
    val schemas: List<String>? = null,
    val Operations: List<ScimPatchOperation>,
)