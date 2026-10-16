package io.github.neronguyen.chat.core.network.model

import io.github.neronguyen.chat.core.model.ChatRoomType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatRoomDto(
    val id: String,
    @SerialName("type") private val rawType: String? = null,
    val name: String? = null,
    val creator: ChatParticipantDto,
    val participants: List<ChatParticipantDto>,
    val lastMessage: ChatMessageDto? = null,
    val lastActivityAt: Instant? = null
) {
    val type: ChatRoomType
        get() = try {
            ChatRoomType.valueOf(rawType.orEmpty().uppercase())
        } catch (_: IllegalArgumentException) {
            ChatRoomType.UNKNOWN
        }
}
