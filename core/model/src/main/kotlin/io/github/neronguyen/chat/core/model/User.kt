package io.github.neronguyen.chat.core.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean
)
