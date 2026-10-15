package io.github.neronguyen.chat.core.network

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import io.github.neronguyen.chat.core.network.model.ChatRoomDto

interface ChatNetworkDataSource {
    suspend fun getChatRoomsForUser(): Either<DataError.Network, List<ChatRoomDto>>

    suspend fun getChatRoom(roomId: String): Either<DataError.Network, ChatRoomDto>

    suspend fun getMessagesForChatRoom(
        roomId: String,
        before: String? = null
    ): Either<DataError.Network, List<ChatMessageDto>>

    suspend fun getParticipantByEmail(email: String): Either<DataError.Network, ChatParticipantDto>

    suspend fun createDirectChatRoom(
        targetUserId: String,
        message: String
    ): Either<DataError.Network, ChatRoomDto>

    suspend fun createGroupChatRoom(
        participantIds: List<String>,
        name: String? = null
    ): Either<DataError.Network, ChatRoomDto>

    suspend fun addParticipantsToGroup(
        roomId: String,
        userIds: List<String>
    ): Either<DataError.Network, ChatRoomDto>

    suspend fun leaveChatRoom(roomId: String): Either<DataError.Network, Unit>

    suspend fun deleteMessage(messageId: String): Either<DataError.Network, Unit>
}
