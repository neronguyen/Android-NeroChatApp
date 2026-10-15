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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomRepositoryImplTest {

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

        override suspend fun updateDeliveryStatus(messageId: String, status: MessageDeliveryStatus) {
            updatedStatuses[messageId] = status
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

        override suspend fun updateMessageStatusAndTimestamp(messageId: String, timestamp: Long, status: MessageDeliveryStatus) {}
    }

    private class FakeChatRealtimeDataSource : ChatRealtimeDataSource {
        var shouldSucceed = true

        override val incomingEvents: Flow<WsServerEvent> = emptyFlow()
        override val connectionState: StateFlow<SocketConnectionState> = MutableStateFlow(SocketConnectionState.Connected)

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
        var lastDirectTargetUserId: String? = null
        var lastDirectMessage: String? = null
        var lastGroupParticipantIds: List<String>? = null
        var lastGroupName: String? = null
        var directChatResponse: Either<DataError.Network, ChatRoomDto> = Either.Left(DataError.Network.Unknown)
        var groupChatResponse: Either<DataError.Network, ChatRoomDto> = Either.Left(DataError.Network.Unknown)
        var participantResponse: Either<DataError.Network, ChatParticipantDto> = Either.Left(DataError.Network.Unknown)

        override suspend fun getChatRoomsForUser(): Either<DataError.Network, List<ChatRoomDto>> = Either.Right(emptyList())
        override suspend fun getChatRoom(roomId: String): Either<DataError.Network, ChatRoomDto> =
            Either.Left(DataError.Network.Unknown)
        override suspend fun getMessagesForChatRoom(
            roomId: String,
            before: String?
        ): Either<DataError.Network, List<ChatMessageDto>> = Either.Right(emptyList())
        override suspend fun getParticipantByEmail(email: String): Either<DataError.Network, ChatParticipantDto> = participantResponse
        override suspend fun createDirectChatRoom(targetUserId: String, message: String): Either<DataError.Network, ChatRoomDto> {
            lastDirectTargetUserId = targetUserId
            lastDirectMessage = message
            return directChatResponse
        }
        override suspend fun createGroupChatRoom(participantIds: List<String>, name: String?): Either<DataError.Network, ChatRoomDto> {
            lastGroupParticipantIds = participantIds
            lastGroupName = name
            return groupChatResponse
        }
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
            user = User(id = "user123", email = "test@example.com", displayName = "Test User", isEmailVerified = true),
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
    fun `findChatParticipantByEmail delegates to networkDataSource`() = runTest {
        val fakeDao = FakeChatDao()
        val fakeRealtime = FakeChatRealtimeDataSource()
        val fakeNetwork = FakeChatNetworkDataSource().apply {
            participantResponse = Either.Right(
                ChatParticipantDto("user-abc", "abc@example.com", "ABC User")
            )
        }
        val fakeToken = FakeTokenDataSource()

        val repository = ChatRoomRepositoryImpl(
            chatDao = fakeDao,
            realtimeDataSource = fakeRealtime,
            networkDataSource = fakeNetwork,
            tokenDataSource = fakeToken,
            defaultDispatcher = testDispatcher
        )

        val result = repository.findChatParticipantByEmail("abc@example.com")

        assertTrue(result.isRight())
        val participant = (result as Either.Right).value
        assertEquals("user-abc", participant.userId)
        assertEquals("abc@example.com", participant.email)
        assertEquals("ABC User", participant.displayName)
    }

    @Test
    fun `createDirectChatRoom passes targetUserId directly to networkDataSource without validation`() = runTest {
        val fakeDao = FakeChatDao()
        val fakeRealtime = FakeChatRealtimeDataSource()
        val targetId = "target-user-id"
        val fakeNetwork = FakeChatNetworkDataSource().apply {
            directChatResponse = Either.Right(
                ChatRoomDto(
                    id = "room1",
                    rawType = "DIRECT",
                    name = null,
                    creator = ChatParticipantDto("user123", "me@example.com", "Me"),
                    participants = listOf(
                        ChatParticipantDto("user123", "me@example.com", "Me"),
                        ChatParticipantDto(targetId, "target@example.com", "Target")
                    ),
                    lastMessage = null,
                    lastActivityAt = Instant.parse("2026-10-10T12:00:00Z")
                )
            )
        }
        val fakeToken = FakeTokenDataSource()

        val repository = ChatRoomRepositoryImpl(
            chatDao = fakeDao,
            realtimeDataSource = fakeRealtime,
            networkDataSource = fakeNetwork,
            tokenDataSource = fakeToken,
            defaultDispatcher = testDispatcher
        )

        val result = repository.createDirectChatRoom(targetId, "hello")

        assertTrue(result.isRight())
        assertEquals(targetId, fakeNetwork.lastDirectTargetUserId)
        assertEquals("hello", fakeNetwork.lastDirectMessage)
    }

    @Test
    fun `createGroupChatRoom passes participantIds directly to networkDataSource without validation`() = runTest {
        val fakeDao = FakeChatDao()
        val fakeRealtime = FakeChatRealtimeDataSource()
        val participantIds = listOf("user-1", "user-2")
        val fakeNetwork = FakeChatNetworkDataSource().apply {
            groupChatResponse = Either.Right(
                ChatRoomDto(
                    id = "group-room-1",
                    rawType = "GROUP",
                    name = "Group Test",
                    creator = ChatParticipantDto("user123", "me@example.com", "Me"),
                    participants = listOf(
                        ChatParticipantDto("user123", "me@example.com", "Me"),
                        ChatParticipantDto("user-1", "u1@example.com", "U1"),
                        ChatParticipantDto("user-2", "u2@example.com", "U2")
                    ),
                    lastMessage = null,
                    lastActivityAt = Instant.parse("2026-10-10T12:00:00Z")
                )
            )
        }
        val fakeToken = FakeTokenDataSource()

        val repository = ChatRoomRepositoryImpl(
            chatDao = fakeDao,
            realtimeDataSource = fakeRealtime,
            networkDataSource = fakeNetwork,
            tokenDataSource = fakeToken,
            defaultDispatcher = testDispatcher
        )

        val result = repository.createGroupChatRoom(participantIds, "  Group Test  ")

        assertTrue(result.isRight())
        assertEquals(participantIds, fakeNetwork.lastGroupParticipantIds)
        assertEquals("Group Test", fakeNetwork.lastGroupName)
    }

    @Test
    fun `sendMessage inserts sending entity and keeps status SENDING on socket write success`() = runTest {
        val fakeDao = FakeChatDao()
        val fakeRealtime = FakeChatRealtimeDataSource().apply { shouldSucceed = true }
        val fakeNetwork = FakeChatNetworkDataSource()
        val fakeToken = FakeTokenDataSource()

        val repository = ChatRoomRepositoryImpl(
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

        val repository = ChatRoomRepositoryImpl(
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

        val repository = ChatRoomRepositoryImpl(
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
