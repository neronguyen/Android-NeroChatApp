package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateDirectChatRequest(
    val targetUserId: String,
    val message: String
)

@Serializable
data class CreateGroupChatRequest(
    val participantIds: List<String>,
    val name: String? = null
)
