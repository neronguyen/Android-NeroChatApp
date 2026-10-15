package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import io.github.neronguyen.chat.core.model.ChatMessage
import io.github.neronguyen.chat.core.model.ChatParticipant
import io.github.neronguyen.chat.core.model.ChatRoom
import io.github.neronguyen.chat.core.model.error.DataError
import kotlinx.coroutines.flow.Flow

interface ChatRoomRepository {
    fun getMessages(roomId: String): Flow<List<ChatMessage>>
    fun getChats(): Flow<List<ChatRoom>>

    suspend fun syncRecentMessages(roomId: String): Either<DataError.Network, Unit>
    suspend fun syncChats(): Either<DataError.Network, Unit>

    suspend fun findChatParticipantByEmail(email: String): Either<DataError.Network, ChatParticipant>

    suspend fun createDirectChatRoom(
        targetUserId: String,
        message: String
    ): Either<DataError.Network, ChatRoom>

    suspend fun createGroupChatRoom(
        participantIds: List<String>,
        name: String? = null
    ): Either<DataError.Network, ChatRoom>

    suspend fun sendMessage(roomId: String, message: String): Either<DataError, Unit>
}
