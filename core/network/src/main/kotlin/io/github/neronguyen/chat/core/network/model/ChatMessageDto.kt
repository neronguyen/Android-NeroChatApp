package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ChatMessageDto(
    val id: String,
    val roomId: String = "",
    val senderId: String,
    val content: String,
    val createdAt: Instant,
    val tempId: String? = null
)
