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
import io.github.neronguyen.chat.core.model.ChatParticipant
import io.github.neronguyen.chat.core.model.ChatRoom
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.ChatNetworkDataSource
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.ChatRoomDto
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
internal class ChatRoomRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val realtimeDataSource: ChatRealtimeDataSource,
    private val networkDataSource: ChatNetworkDataSource,
    private val tokenDataSource: TokenDataSource,
    @Dispatcher(ChatDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) : ChatRoomRepository {

    override fun getMessages(chatRoomId: String): Flow<List<ChatMessage>> {
        return chatDao.getRecentMessages(chatRoomId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(defaultDispatcher)
    }

    override fun getChats(): Flow<List<ChatRoom>> {
        return chatDao.getChatsWithDetails().map { relations ->
            relations.map { it.toDomain() }
        }.flowOn(defaultDispatcher)
    }

    override suspend fun syncRecentMessages(chatRoomId: String): Either<DataError.Network, Unit> {
        return networkDataSource.getMessagesForChatRoom(chatRoomId).map { dtos ->
            chatDao.upsertMessages(dtos.map { it.toEntity(MessageDeliveryStatus.SENT) })
        }
    }

    override suspend fun syncChats(): Either<DataError.Network, Unit> {
        return networkDataSource.getChatRoomsForUser().map { dtos ->
            dtos.forEach { dto ->
                saveChatRoomDto(dto)
            }
        }
    }

    override suspend fun findChatParticipantByEmail(email: String): Either<DataError.Network, ChatParticipant> {
        return networkDataSource.getParticipantByEmail(email).map { it.toDomain() }
    }

    override suspend fun createDirectChatRoom(
        targetUserId: String,
        message: String
    ): Either<DataError.Network, ChatRoom> {
        return networkDataSource.createDirectChatRoom(targetUserId, message).map { dto ->
            saveChatRoomDto(dto)
            dto.toDomain()
        }
    }

    override suspend fun createGroupChatRoom(
        participantIds: List<String>,
        name: String?
    ): Either<DataError.Network, ChatRoom> {
        val trimmedName = name?.trim()?.ifEmpty { null }
        return networkDataSource.createGroupChatRoom(participantIds, trimmedName).map { dto ->
            saveChatRoomDto(dto)
            dto.toDomain()
        }
    }

    private suspend fun saveChatRoomDto(dto: ChatRoomDto) {
        chatDao.insertFullChat(
            room = dto.toEntity(),
            creator = dto.creator.toEntity(),
            participants = dto.participants.map { it.toEntity() },
            lastMessage = dto.lastMessage?.toEntity(MessageDeliveryStatus.SENT)
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        chatRoomId: String,
        message: String
    ): Either<DataError, Unit> {
        val auth = tokenDataSource.authState.firstOrNull() as? AuthState.Authenticated
            ?: return Either.Left(DataError.Local.Unauthorized)

        val messageId = Uuid.generateV7().toString()
        val now = System.currentTimeMillis()

        val sendingEntity = ChatMessageEntity(
            id = messageId,
            chatRoomId = chatRoomId,
            senderId = auth.user.id,
            content = message,
            timestamp = now,
            deliveryStatus = MessageDeliveryStatus.SENDING
        )

        chatDao.upsertMessage(sendingEntity)

        return realtimeDataSource.sendMessage(
            WsClientMessage.NewMessage(
                chatRoomId = chatRoomId,
                content = message,
                messageId = messageId
            )
        ).onLeft {
            chatDao.updateDeliveryStatus(messageId, MessageDeliveryStatus.FAILED)
        }
    }
}
