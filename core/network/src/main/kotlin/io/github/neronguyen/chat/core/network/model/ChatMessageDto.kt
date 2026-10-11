package io.github.neronguyen.chat.core.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatMessageDto(
    val id: String,
    @JsonNames("chatRoomId", "chatId")
    @SerialName("chatRoomId")
    val chatRoomId: String = "",
    val senderId: String,
    val content: String,
    val createdAt: Instant,
    val tempId: String? = null
)
