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
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyen.chat.core.network.model.websocket.WsClientMessage
import io.github.neronguyen.chat.core.network.model.websocket.WsServerEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeConnectionManagerTest {

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
        var reconciledAck: Triple<String, String, Long>? = null

        override fun getChatsWithDetails(): Flow<List<ChatRoomWithDetails>> = emptyFlow()
        override fun getChatWithDetails(chatRoomId: String): Flow<ChatRoomWithDetails?> = emptyFlow()
        override fun getRecentMessages(chatRoomId: String, limit: Int): Flow<List<ChatMessageEntity>> = emptyFlow()
        override suspend fun getMessageById(id: String): ChatMessageEntity? = null

        override suspend fun upsertRoom(room: ChatRoomEntity) {}
        override suspend fun upsertRooms(rooms: List<ChatRoomEntity>) {}
        override suspend fun upsertParticipant(participant: ChatParticipantEntity) {}
        override suspend fun upsertParticipants(participants: List<ChatParticipantEntity>) {}
        override suspend fun upsertCrossRefs(crossRefs: List<ChatRoomParticipantCrossRef>) {}
        override suspend fun upsertMessage(message: ChatMessageEntity) {}

        override suspend fun upsertMessages(messages: List<ChatMessageEntity>) {
            upsertedMessages.addAll(messages)
        }

        override suspend fun updateDeliveryStatus(messageId: String, status: MessageDeliveryStatus) {}
        override suspend fun deleteMessage(messageId: String) {}

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
            appScope = externalScope
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
            appScope = externalScope
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
        assertEquals(1, fakeDao.upsertedMessages.size)
        assertEquals("msg-123", fakeDao.upsertedMessages[0].id)
        assertEquals("Hello", fakeDao.upsertedMessages[0].content)
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
            appScope = externalScope
        )

        val owner = FakeLifecycleOwner()
        manager.onStart(owner)
        manager.onStop(owner)

        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeDataSource.disconnectCalledCount)
    }
}
