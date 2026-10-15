package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.database.entity.ChatParticipantEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomParticipantCrossRef
import io.github.neronguyen.chat.core.database.model.ChatRoomWithDetails
import io.github.neronguyen.chat.core.datastore.TokenDataSource
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.ChatNetworkDataSource
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import io.github.neronguyen.chat.core.network.model.ChatRoomDto
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyen.chat.core.network.model.websocket.WsClientMessage
import io.github.neronguyen.chat.core.network.model.websocket.WsServerEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessageRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeChatDao : ChatDao {
        val insertedMessages = mutableListOf<ChatMessageEntity>()
        val updatedStatuses = mutableMapOf<String, MessageDeliveryStatus>()
        var lastUpdatedRoomId: String? = null
        var lastUpdatedRoomMessageId: String? = null
        var lastUpdatedRoomTimestamp: Long? = null

        override fun getChatsWithDetails(): Flow<List<ChatRoomWithDetails>> = emptyFlow()
        override fun getChatWithDetails(roomId: String): Flow<ChatRoomWithDetails?> = emptyFlow()
        override fun getRecentMessages(roomId: String, limit: Int): Flow<List<ChatMessageEntity>> =
            emptyFlow()

        override suspend fun getMessageById(id: String): ChatMessageEntity? = null
        override suspend fun getCrossRefsForRooms(roomIds: List<String>): List<ChatRoomParticipantCrossRef> =
            emptyList()

        override suspend fun deleteCrossRefs(crossRefs: List<ChatRoomParticipantCrossRef>) {}

        override suspend fun upsertRoom(room: ChatRoomEntity) {}
        override suspend fun upsertRooms(rooms: List<ChatRoomEntity>) {}
        override suspend fun upsertParticipant(participant: ChatParticipantEntity) {}
        override suspend fun upsertParticipants(participants: List<ChatParticipantEntity>) {}
        override suspend fun upsertCrossRefs(crossRefs: List<ChatRoomParticipantCrossRef>) {}
        override suspend fun upsertMessage(message: ChatMessageEntity) {
            insertedMessages.add(message)
        }

        override suspend fun upsertMessages(messages: List<ChatMessageEntity>) {}

        override suspend fun updateDeliveryStatus(
            messageId: String,
            status: MessageDeliveryStatus
        ) {
            updatedStatuses[messageId] = status
        }

        override suspend fun markSendingMessagesAsFailed(
            sendingStatus: MessageDeliveryStatus,
            failedStatus: MessageDeliveryStatus
        ) {
        }

        override suspend fun insertSendingMessage(message: ChatMessageEntity) {
            insertedMessages.add(message)
            lastUpdatedRoomId = message.roomId
            lastUpdatedRoomMessageId = message.id
            lastUpdatedRoomTimestamp = message.timestamp
        }

        override suspend fun upsertIncomingOrAckedMessage(message: ChatMessageEntity) {
            insertedMessages.add(message)
            lastUpdatedRoomId = message.roomId
            lastUpdatedRoomMessageId = message.id
            lastUpdatedRoomTimestamp = message.timestamp
        }

        override suspend fun deleteMessage(messageId: String) {}

        override suspend fun deleteParticipantCrossRef(roomId: String, userId: String) {}

        override suspend fun handleParticipantJoined(
            roomId: String,
            newUsers: List<ChatParticipantEntity>,
            addedBy: ChatParticipantEntity?
        ) {
        }

        override suspend fun handleParticipantLeft(
            roomId: String,
            leftUser: ChatParticipantEntity,
            kickedBy: ChatParticipantEntity?
        ) {
        }

        override suspend fun updateRoomLastActivity(
            roomId: String,
            messageId: String,
            timestamp: Long
        ) {
            lastUpdatedRoomId = roomId
            lastUpdatedRoomMessageId = messageId
            lastUpdatedRoomTimestamp = timestamp
        }
    }

    private class FakeChatRealtimeDataSource : ChatRealtimeDataSource {
        var shouldSucceed = true

        override val incomingEvents: Flow<WsServerEvent> = emptyFlow()
        override val connectionState: StateFlow<SocketConnectionState> =
            MutableStateFlow(SocketConnectionState.Connected)

        override suspend fun connect() {}
        override suspend fun disconnect() {}

        override suspend fun sendMessage(
            message: WsClientMessage
        ): Either<DataError.Realtime, Unit> {
            return if (shouldSucceed) {
                Either.Right(Unit)
            } else {
                Either.Left(DataError.Realtime.SocketError)
            }
        }
    }

    private class FakeChatNetworkDataSource : ChatNetworkDataSource {
        override suspend fun getChatRoomsForUser(): Either<DataError.Network, List<ChatRoomDto>> =
            Either.Right(emptyList())

        override suspend fun getChatRoom(roomId: String): Either<DataError.Network, ChatRoomDto> =
            Either.Left(DataError.Network.Unknown)

        override suspend fun getMessagesForChatRoom(
            roomId: String,
            before: String?
        ): Either<DataError.Network, List<ChatMessageDto>> = Either.Right(emptyList())

        override suspend fun getParticipantByEmail(email: String): Either<DataError.Network, ChatParticipantDto> =
            Either.Left(DataError.Network.Unknown)

        override suspend fun createDirectChatRoom(
            targetUserId: String,
            message: String
        ): Either<DataError.Network, ChatRoomDto> = Either.Left(DataError.Network.Unknown)

        override suspend fun createGroupChatRoom(
            participantIds: List<String>,
            name: String?
        ): Either<DataError.Network, ChatRoomDto> = Either.Left(DataError.Network.Unknown)

        override suspend fun addParticipantsToGroup(
            roomId: String,
            userIds: List<String>
        ): Either<DataError.Network, ChatRoomDto> = Either.Left(DataError.Network.Unknown)

        override suspend fun leaveChatRoom(roomId: String): Either<DataError.Network, Unit> =
            Either.Right(Unit)

        override suspend fun deleteMessage(messageId: String): Either<DataError.Network, Unit> =
            Either.Right(Unit)
    }

    private class FakeTokenDataSource(
        initialAuthState: AuthState = AuthState.Authenticated(
            user = User(
                id = "user123",
                email = "test@example.com",
                displayName = "Test User",
                isEmailVerified = true
            ),
            accessToken = "access_token"
        )
    ) : TokenDataSource {
        override val authState: Flow<AuthState> = flowOf(initialAuthState)
        override suspend fun getRefreshToken(): String? = "refresh_token"
        override suspend fun getAccessToken(): String? = "access_token"
        override suspend fun saveAuthData(user: User, accessToken: String, refreshToken: String) {}
        override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
        override suspend fun clearAuthData() {}
    }

    @Test
    fun `sendMessage inserts sending entity and keeps status SENDING on socket write success`() =
        runTest {
            val fakeDao = FakeChatDao()
            val fakeRealtime = FakeChatRealtimeDataSource().apply { shouldSucceed = true }
            val fakeNetwork = FakeChatNetworkDataSource()
            val fakeToken = FakeTokenDataSource()

            val repository = ChatMessageRepositoryImpl(
                chatDao = fakeDao,
                realtimeDataSource = fakeRealtime,
                networkDataSource = fakeNetwork,
                tokenDataSource = fakeToken,
                defaultDispatcher = testDispatcher
            )

            val result = repository.sendMessage("room1", "Hello World")

            assertTrue(result.isRight())
            assertEquals(1, fakeDao.insertedMessages.size)

            val insertedMsg = fakeDao.insertedMessages.first()
            assertEquals("room1", insertedMsg.roomId)
            assertEquals("Hello World", insertedMsg.content)
            assertEquals(MessageDeliveryStatus.SENDING, insertedMsg.deliveryStatus)

            assertEquals("room1", fakeDao.lastUpdatedRoomId)
            assertEquals(insertedMsg.id, fakeDao.lastUpdatedRoomMessageId)
            assertEquals(insertedMsg.timestamp, fakeDao.lastUpdatedRoomTimestamp)

            assertNull(fakeDao.updatedStatuses[insertedMsg.id])
        }

    @Test
    fun `sendMessage updates status to FAILED when socket write fails`() = runTest {
        val fakeDao = FakeChatDao()
        val fakeRealtime = FakeChatRealtimeDataSource().apply { shouldSucceed = false }
        val fakeNetwork = FakeChatNetworkDataSource()
        val fakeToken = FakeTokenDataSource()

        val repository = ChatMessageRepositoryImpl(
            chatDao = fakeDao,
            realtimeDataSource = fakeRealtime,
            networkDataSource = fakeNetwork,
            tokenDataSource = fakeToken,
            defaultDispatcher = testDispatcher
        )

        val result = repository.sendMessage("room1", "Hello World")

        assertTrue(result.isLeft())
        assertEquals(DataError.Realtime.SocketError, result.leftOrNull())
        assertEquals(1, fakeDao.insertedMessages.size)

        val insertedMsg = fakeDao.insertedMessages.first()
        assertEquals(MessageDeliveryStatus.FAILED, fakeDao.updatedStatuses[insertedMsg.id])
    }

    @Test
    fun `sendMessage returns Unauthorized error when unauthenticated`() = runTest {
        val fakeDao = FakeChatDao()
        val fakeRealtime = FakeChatRealtimeDataSource()
        val fakeNetwork = FakeChatNetworkDataSource()
        val fakeToken = FakeTokenDataSource(initialAuthState = AuthState.Unauthenticated)

        val repository = ChatMessageRepositoryImpl(
            chatDao = fakeDao,
            realtimeDataSource = fakeRealtime,
            networkDataSource = fakeNetwork,
            tokenDataSource = fakeToken,
            defaultDispatcher = testDispatcher
        )

        val result = repository.sendMessage("room1", "Hello World")

        assertTrue(result.isLeft())
        assertEquals(DataError.Local.Unauthorized, result.leftOrNull())
        assertEquals(0, fakeDao.insertedMessages.size)
    }
}
