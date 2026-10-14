package io.github.neronguyen.chat.core.data.realtime

import androidx.lifecycle.LifecycleOwner
import arrow.core.Either
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.database.entity.ChatParticipantEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomParticipantCrossRef
import io.github.neronguyen.chat.core.database.model.ChatRoomWithDetails
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyen.chat.core.network.model.websocket.WsClientMessage
import io.github.neronguyen.chat.core.network.model.websocket.WsServerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeConnectionManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeChatRealtimeDataSource : ChatRealtimeDataSource {
        val incomingEventsFlow = MutableSharedFlow<WsServerEvent>()
        val connectionStateFlow = MutableStateFlow<SocketConnectionState>(SocketConnectionState.Disconnected)

        var connectCalledCount = 0
        var disconnectCalledCount = 0

        override val incomingEvents: Flow<WsServerEvent> = incomingEventsFlow
        override val connectionState: StateFlow<SocketConnectionState> = connectionStateFlow

        override suspend fun connect() {
            connectCalledCount++
            connectionStateFlow.value = SocketConnectionState.Connected
        }

        override suspend fun disconnect() {
            disconnectCalledCount++
            connectionStateFlow.value = SocketConnectionState.Disconnected
        }

        override suspend fun sendMessage(
            message: WsClientMessage
        ): Either<DataError.Network, Unit> {
            return Either.Right(Unit)
        }
    }

    private class FakeChatDao : ChatDao {
        val upsertedMessages = mutableListOf<ChatMessageEntity>()
        val upsertedParticipants = mutableListOf<ChatParticipantEntity>()
        val upsertedCrossRefs = mutableListOf<ChatRoomParticipantCrossRef>()
        val deletedMessages = mutableListOf<String>()
        val deletedCrossRefs = mutableListOf<Pair<String, String>>()
        var reconciledAck: Triple<String, String, Long>? = null

        override fun getChatsWithDetails(): Flow<List<ChatRoomWithDetails>> = emptyFlow()
        override fun getChatWithDetails(chatRoomId: String): Flow<ChatRoomWithDetails?> = emptyFlow()
        override fun getRecentMessages(chatRoomId: String, limit: Int): Flow<List<ChatMessageEntity>> = emptyFlow()
        override suspend fun getMessageById(id: String): ChatMessageEntity? = null

        override suspend fun upsertRoom(room: ChatRoomEntity) {}
        override suspend fun upsertRooms(rooms: List<ChatRoomEntity>) {}
        override suspend fun upsertParticipant(participant: ChatParticipantEntity) {
            upsertedParticipants.add(participant)
        }

        override suspend fun upsertParticipants(participants: List<ChatParticipantEntity>) {
            upsertedParticipants.addAll(participants)
        }

        override suspend fun upsertCrossRefs(crossRefs: List<ChatRoomParticipantCrossRef>) {
            upsertedCrossRefs.addAll(crossRefs)
        }
        override suspend fun upsertMessage(message: ChatMessageEntity) {}

        override suspend fun upsertMessages(messages: List<ChatMessageEntity>) {
            upsertedMessages.addAll(messages)
        }

        val updatedStatuses = mutableMapOf<String, MessageDeliveryStatus>()

        override suspend fun updateDeliveryStatus(
            messageId: String,
            status: MessageDeliveryStatus
        ) {
            updatedStatuses[messageId] = status
        }

        override suspend fun deleteMessage(messageId: String) {
            deletedMessages.add(messageId)
        }

        override suspend fun deleteParticipantCrossRef(chatRoomId: String, userId: String) {
            deletedCrossRefs.add(chatRoomId to userId)
        }

        override suspend fun handleParticipantJoined(
            chatRoomId: String,
            newUsers: List<ChatParticipantEntity>,
            addedBy: ChatParticipantEntity?
        ) {
            addedBy?.let { upsertedParticipants.add(it) }
            if (newUsers.isNotEmpty()) {
                upsertedParticipants.addAll(newUsers)
                val crossRefs = newUsers.map { participant ->
                    ChatRoomParticipantCrossRef(chatId = chatRoomId, userId = participant.userId)
                }
                upsertedCrossRefs.addAll(crossRefs)
            }
        }

        override suspend fun handleParticipantLeft(
            chatRoomId: String,
            leftUser: ChatParticipantEntity,
            kickedBy: ChatParticipantEntity?
        ) {
            kickedBy?.let { upsertedParticipants.add(it) }
            deletedCrossRefs.add(chatRoomId to leftUser.userId)
        }

        override suspend fun updateRoomLastActivity(chatRoomId: String, messageId: String, timestamp: Long) {}

        override suspend fun updateMessageStatusAndTimestamp(messageId: String, timestamp: Long, status: MessageDeliveryStatus) {}

        override suspend fun reconcileMessageAck(
            chatRoomId: String,
            messageId: String,
            serverTimestamp: Long,
            status: MessageDeliveryStatus
        ) {
            reconciledAck = Triple(chatRoomId, messageId, serverTimestamp)
        }
    }

    private class FakeLifecycleOwner : LifecycleOwner {
        override val lifecycle get() = error("Not needed for unit tests")
    }

    @Test
    fun `onStart connects realtime data source and handles incoming NewMessage with tempId`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val externalScope = TestScope(testDispatcher)
        val fakeDataSource = FakeChatRealtimeDataSource()
        val fakeDao = FakeChatDao()

        val manager = RealtimeConnectionManager(
            realtimeDataSource = fakeDataSource,
            chatDao = fakeDao,
            appScope = externalScope,
            ioDispatcher = testDispatcher
        )

        val owner = FakeLifecycleOwner()
        manager.onStart(owner)

        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeDataSource.connectCalledCount)

        val messageDto = ChatMessageDto(
            id = "server-456",
            tempId = "temp-123",
            chatRoomId = "room-1",
            senderId = "user-1",
            content = "Hello",
            createdAt = Instant.fromEpochMilliseconds(1000L)
        )

        fakeDataSource.incomingEventsFlow.emit(WsServerEvent.NewMessage(messageDto))

        testScheduler.advanceUntilIdle()
        assertEquals(Triple("room-1", "server-456", 1000L), fakeDao.reconciledAck)
    }

    @Test
    fun `onStart handles incoming NewMessage event without tempId and upserts message`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val externalScope = TestScope(testDispatcher)
        val fakeDataSource = FakeChatRealtimeDataSource()
        val fakeDao = FakeChatDao()

        val manager = RealtimeConnectionManager(
            realtimeDataSource = fakeDataSource,
            chatDao = fakeDao,
            appScope = externalScope,
            ioDispatcher = testDispatcher
        )

        val owner = FakeLifecycleOwner()
        manager.onStart(owner)

        val messageDto = ChatMessageDto(
            id = "msg-123",
            chatRoomId = "room-1",
            senderId = "user-1",
            content = "Hello",
            createdAt = Instant.fromEpochMilliseconds(2000L)
        )

        fakeDataSource.incomingEventsFlow.emit(WsServerEvent.NewMessage(messageDto))

        testScheduler.advanceUntilIdle()
        assertEquals(Triple("room-1", "msg-123", 2000L), fakeDao.reconciledAck)
    }

    @Test
    fun `onStart handles MessageDeleted event and updates message status to DELETED in DAO`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            val externalScope = TestScope(testDispatcher)
            val fakeDataSource = FakeChatRealtimeDataSource()
            val fakeDao = FakeChatDao()

            val manager = RealtimeConnectionManager(
                realtimeDataSource = fakeDataSource,
                chatDao = fakeDao,
                appScope = externalScope,
                ioDispatcher = testDispatcher
            )

            val owner = FakeLifecycleOwner()
            manager.onStart(owner)

            fakeDataSource.incomingEventsFlow.emit(
                WsServerEvent.MessageDeleted(chatRoomId = "room-1", messageId = "msg-999")
            )

            testScheduler.advanceUntilIdle()
            assertEquals(MessageDeliveryStatus.DELETED, fakeDao.updatedStatuses["msg-999"])
        }

    @Test
    fun `onStart handles ParticipantJoined event and upserts participants and cross refs`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            val externalScope = TestScope(testDispatcher)
            val fakeDataSource = FakeChatRealtimeDataSource()
            val fakeDao = FakeChatDao()

            val manager = RealtimeConnectionManager(
                realtimeDataSource = fakeDataSource,
                chatDao = fakeDao,
                appScope = externalScope,
                ioDispatcher = testDispatcher
            )

            val owner = FakeLifecycleOwner()
            manager.onStart(owner)

            val newParticipant = ChatParticipantDto(
                userId = "user-2",
                email = "p2@test.com",
                displayName = "Participant 2"
            )
            val addedBy = ChatParticipantDto(
                userId = "user-1",
                email = "p1@test.com",
                displayName = "Participant 1"
            )

            fakeDataSource.incomingEventsFlow.emit(
                WsServerEvent.ParticipantJoined(
                    chatRoomId = "room-1",
                    newUsers = listOf(newParticipant),
                    addedBy = addedBy
                )
            )

            testScheduler.advanceUntilIdle()
            assertEquals(2, fakeDao.upsertedParticipants.size)
            assertEquals(1, fakeDao.upsertedCrossRefs.size)
            assertEquals("room-1", fakeDao.upsertedCrossRefs[0].chatId)
            assertEquals("user-2", fakeDao.upsertedCrossRefs[0].userId)
        }

    @Test
    fun `onStart handles ParticipantLeft event and deletes participant cross ref`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val externalScope = TestScope(testDispatcher)
        val fakeDataSource = FakeChatRealtimeDataSource()
        val fakeDao = FakeChatDao()

        val manager = RealtimeConnectionManager(
            realtimeDataSource = fakeDataSource,
            chatDao = fakeDao,
            appScope = externalScope,
            ioDispatcher = testDispatcher
        )

        val owner = FakeLifecycleOwner()
        manager.onStart(owner)

        val leftUser = ChatParticipantDto(
            userId = "user-2",
            email = "p2@test.com",
            displayName = "Participant 2"
        )

        fakeDataSource.incomingEventsFlow.emit(
            WsServerEvent.ParticipantLeft(
                chatRoomId = "room-1",
                leftUser = leftUser
            )
        )

        testScheduler.advanceUntilIdle()
        assertEquals(listOf("room-1" to "user-2"), fakeDao.deletedCrossRefs)
    }

    @Test
    fun `onStop disconnects realtime data source`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val externalScope = TestScope(testDispatcher)
        val fakeDataSource = FakeChatRealtimeDataSource()
        val fakeDao = FakeChatDao()

        val manager = RealtimeConnectionManager(
            realtimeDataSource = fakeDataSource,
            chatDao = fakeDao,
            appScope = externalScope,
            ioDispatcher = testDispatcher
        )

        val owner = FakeLifecycleOwner()
        manager.onStart(owner)
        manager.onStop(owner)

        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeDataSource.disconnectCalledCount)
    }
}
