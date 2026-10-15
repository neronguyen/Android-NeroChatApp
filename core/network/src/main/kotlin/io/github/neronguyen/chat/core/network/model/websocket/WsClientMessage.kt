package io.github.neronguyen.chat.core.network.model.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsClientMessage {

    @Serializable
    @SerialName("new_message")
    data class NewMessage(
        val roomId: String,
        val content: String,
        val messageId: String? = null
    ) : WsClientMessage()
}
