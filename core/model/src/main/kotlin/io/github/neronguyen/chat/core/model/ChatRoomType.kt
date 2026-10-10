package io.github.neronguyen.chat.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRoomType {
    DIRECT,
    GROUP
}
