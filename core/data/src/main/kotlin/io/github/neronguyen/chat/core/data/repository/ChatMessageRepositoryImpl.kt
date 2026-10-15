package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import io.github.neronguyen.chat.core.common.network.ChatDispatchers
import io.github.neronguyen.chat.core.common.network.Dispatcher
import io.github.neronguyen.chat.core.data.mapper.toDomain
import io.github.neronguyen.chat.core.data.mapper.toEntity
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.datastore.TokenDataSource
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.ChatMessage
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.ChatNetworkDataSource
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.websocket.WsClientMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Singleton
internal class ChatMessageRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val realtimeDataSource: ChatRealtimeDataSource,
    private val networkDataSource: ChatNetworkDataSource,
    private val tokenDataSource: TokenDataSource,
    @Dispatcher(ChatDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) : ChatMessageRepository {

    override fun getMessages(roomId: String): Flow<List<ChatMessage>> {
        return chatDao.getRecentMessages(roomId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(defaultDispatcher)
    }

    override suspend fun syncRecentMessages(roomId: String): Either<DataError.Network, Unit> {
        return networkDataSource.getMessagesForChatRoom(roomId).map { dtos ->
            chatDao.upsertMessages(dtos.map { it.toEntity(MessageDeliveryStatus.SENT) })
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        roomId: String,
        message: String
    ): Either<DataError, Unit> {
        val auth = tokenDataSource.authState.firstOrNull() as? AuthState.Authenticated
            ?: return Either.Left(DataError.Local.Unauthorized)

        val messageId = Uuid.generateV7().toString()
        val now = System.currentTimeMillis()

        val sendingEntity = ChatMessageEntity(
            id = messageId,
            roomId = roomId,
            senderId = auth.user.id,
            content = message,
            timestamp = now,
            deliveryStatus = MessageDeliveryStatus.SENDING
        )

        chatDao.insertSendingMessage(sendingEntity)

        return realtimeDataSource.sendMessage(
            WsClientMessage.NewMessage(
                roomId = roomId,
                content = message,
                messageId = messageId
            )
        ).onLeft {
            chatDao.updateDeliveryStatus(messageId, MessageDeliveryStatus.FAILED)
        }
    }
}
