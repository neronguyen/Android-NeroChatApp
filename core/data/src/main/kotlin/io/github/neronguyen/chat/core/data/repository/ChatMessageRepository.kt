package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import io.github.neronguyen.chat.core.model.ChatMessage
import io.github.neronguyen.chat.core.model.error.DataError
import kotlinx.coroutines.flow.Flow

interface ChatMessageRepository {
    fun getMessages(roomId: String): Flow<List<ChatMessage>>
    suspend fun syncRecentMessages(roomId: String): Either<DataError.Network, Unit>
    suspend fun sendMessage(roomId: String, message: String): Either<DataError, Unit>
}
