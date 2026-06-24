package com.study.studyscim.presentation.scim.dto

data class ScimName(
    val formatted: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
)

data class ScimEmail(
    val value: String,
    val type: String = "work",
    val primary: Boolean = true,
)

data class ScimMeta(
    val resourceType: String,
    val created: String,
    val lastModified: String,
    val location: String,
    val version: String,
)

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

// Resources は SCIM RFC 7644 仕様で大文字始まりが必須
data class ScimListResponse<T>(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
    val totalResults: Int,
    val startIndex: Int = 1,
    val itemsPerPage: Int,
    val Resources: List<T>,
)

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

data class ScimError(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
    val status: String,
    val scimType: String? = null,
    val detail: String,
)
