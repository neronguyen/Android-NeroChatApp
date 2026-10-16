package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class AddChatParticipantRequest(
    val userIds: List<String>
)
