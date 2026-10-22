package io.github.neronguyen.chat.core.data.realtime

import io.github.neronguyen.chat.core.common.network.AppScope
import io.github.neronguyen.chat.core.common.network.ChatDispatchers
import io.github.neronguyen.chat.core.common.network.Dispatcher
import io.github.neronguyen.chat.core.data.mapper.toEntity
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState.Disconnected
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState.Failed
import io.github.neronguyenvn.nerochat.contract.chat.OutgoingWsMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class ChatRealtimeSyncManager @Inject constructor(
    private val realtimeDataSource: ChatRealtimeDataSource,
    private val chatDao: ChatDao,
    @AppScope private val appScope: CoroutineScope,
    @Dispatcher(ChatDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val initialized = AtomicBoolean(false)

    fun initialize() {
        if (!initialized.compareAndSet(expectedValue = false, newValue = true)) return

        appScope.launch(ioDispatcher) {
            realtimeDataSource.incomingEvents.collect { event ->
                when (event) {
                    is OutgoingWsMessage.NewMessage -> handleNewMessage(event)
                    is OutgoingWsMessage.MessageDeleted -> handleMessageDeleted(event)
                    is OutgoingWsMessage.ParticipantJoined -> handleParticipantJoined(event)
                    is OutgoingWsMessage.ParticipantLeft -> handleParticipantLeft(event)
                    is OutgoingWsMessage.Error -> {
                        // Ignore or log error
                    }
                }
            }
        }

        appScope.launch(ioDispatcher) {
            realtimeDataSource.connectionState.collect { state ->
                if (state is Failed || state is Disconnected) {
                    chatDao.markSendingMessagesAsFailed()
                }
            }
        }
    }

    private suspend fun handleNewMessage(event: OutgoingWsMessage.NewMessage) {
        val entity = event.message.toEntity(status = MessageDeliveryStatus.SENT)
        chatDao.upsertIncomingOrAckedMessage(entity)
    }

    private suspend fun handleMessageDeleted(event: OutgoingWsMessage.MessageDeleted) {
        chatDao.updateDeliveryStatus(event.messageId, MessageDeliveryStatus.DELETED)
    }

    private suspend fun handleParticipantJoined(event: OutgoingWsMessage.ParticipantJoined) {
        chatDao.handleParticipantJoined(
            roomId = event.roomId,
            newUsers = event.newUsers.map { it.toEntity() },
            addedBy = event.addedBy?.toEntity()
        )
    }

    private suspend fun handleParticipantLeft(event: OutgoingWsMessage.ParticipantLeft) {
        chatDao.handleParticipantLeft(
            roomId = event.roomId,
            leftUser = event.leftUser.toEntity(),
            kickedBy = event.kickedBy?.toEntity()
        )
    }
}
