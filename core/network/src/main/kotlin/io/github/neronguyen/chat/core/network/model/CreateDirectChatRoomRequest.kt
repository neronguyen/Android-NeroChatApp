package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateDirectChatRoomRequest(
    val targetUserId: String,
    val message: String
)
