package com.study.studyscim.presentation.scim.shared

data class ScimError(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:Error"),
    val status: String,
    val scimType: String? = null,
    val detail: String,
)

// Resources は SCIM RFC 7644 仕様で大文字始まりが必須
data class ScimListResponse<T>(
    val schemas: List<String> = listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
    val totalResults: Int,
    val startIndex: Int = 1,
    val itemsPerPage: Int,
    val Resources: List<T>,
)