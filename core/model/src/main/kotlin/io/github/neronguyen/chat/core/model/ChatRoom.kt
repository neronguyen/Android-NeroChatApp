package io.github.neronguyen.chat.core.model

import kotlin.time.Instant

data class ChatRoom(
    val id: String,
    val type: ChatRoomType,
    val name: String? = null,
    val creator: ChatParticipant,
    val participants: List<ChatParticipant>,
    val lastMessage: ChatMessage? = null,
    val lastActivityAt: Instant? = null
)
