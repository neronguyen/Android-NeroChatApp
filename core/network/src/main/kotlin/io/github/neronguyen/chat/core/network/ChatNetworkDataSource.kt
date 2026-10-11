package io.github.neronguyen.chat.core.network

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import io.github.neronguyen.chat.core.network.model.ChatRoomDto

interface ChatNetworkDataSource {
    suspend fun getChatRoomsForUser(): Either<DataError.Network, List<ChatRoomDto>>
    suspend fun getChatRoom(chatRoomId: String): Either<DataError.Network, ChatRoomDto>

    // TODO Pagination
    suspend fun getMessagesForChatRoom(chatRoomId: String): Either<DataError.Network, List<ChatMessageDto>>

    suspend fun getParticipantByEmail(email: String): Either<DataError.Network, ChatParticipantDto>

    // TODO Pass generated messageId to server to sync with realtime message
    suspend fun createDirectChatRoom(
        targetUserId: String,
        message: String
    ): Either<DataError.Network, ChatRoomDto>

    suspend fun createGroupChatRoom(
        participantIds: List<String>,
        name: String? = null
    ): Either<DataError.Network, ChatRoomDto>
}
