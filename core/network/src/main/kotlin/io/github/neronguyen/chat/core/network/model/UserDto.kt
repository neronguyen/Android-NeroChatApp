package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean
)
