package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupChatRoomRequest(
    val name: String,
    val participantIds: List<String>
)
