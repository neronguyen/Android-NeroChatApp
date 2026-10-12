package io.github.neronguyen.chat.core.network.model.websocket

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class WsClientMessage {

    @Serializable
    @SerialName("new_message")
    data class NewMessage(
        @JsonNames("chatRoomId", "chatId")
        @SerialName("chatRoomId")
        val chatRoomId: String,
        val content: String,
        val messageId: String? = null
    ) : WsClientMessage()
}
