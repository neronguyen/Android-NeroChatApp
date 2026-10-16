package io.github.neronguyen.chat.core.model

data class ChatParticipant(
    val userId: String,
    val email: String,
    val displayName: String,
    val profilePictureUrl: String? = null
)
