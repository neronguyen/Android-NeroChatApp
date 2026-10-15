package io.github.neronguyen.chat.core.network.model.websocket

import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsServerEvent {

    @Serializable
    @SerialName("new_message")
    data class NewMessage(
        val message: ChatMessageDto
    ) : WsServerEvent()

    @Serializable
    @SerialName("message_deleted")
    data class MessageDeleted(
        val roomId: String,
        val messageId: String
    ) : WsServerEvent()

    @Serializable
    @SerialName("participant_joined")
    data class ParticipantJoined(
        val roomId: String,
        val newUsers: List<ChatParticipantDto>,
        val addedBy: ChatParticipantDto? = null
    ) : WsServerEvent()

    @Serializable
    @SerialName("participant_left")
    data class ParticipantLeft(
        val roomId: String,
        val leftUser: ChatParticipantDto,
        val kickedBy: ChatParticipantDto? = null
    ) : WsServerEvent()

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : WsServerEvent()
}
