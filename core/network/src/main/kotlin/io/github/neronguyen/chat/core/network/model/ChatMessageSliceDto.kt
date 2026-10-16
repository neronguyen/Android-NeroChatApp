package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageSliceDto(
    val messages: List<ChatMessageDto>,
    val hasMore: Boolean
)
