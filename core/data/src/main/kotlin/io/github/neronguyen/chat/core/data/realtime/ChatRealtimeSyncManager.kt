package io.github.neronguyen.chat.core.data.realtime

import io.github.neronguyen.chat.core.common.network.AppScope
import io.github.neronguyen.chat.core.common.network.ChatDispatchers
import io.github.neronguyen.chat.core.common.network.Dispatcher
import io.github.neronguyen.chat.core.data.mapper.toEntity
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.websocket.WsServerEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRealtimeSyncManager @Inject constructor(
    private val realtimeDataSource: ChatRealtimeDataSource,
    private val chatDao: ChatDao,
    @AppScope private val appScope: CoroutineScope,
    @Dispatcher(ChatDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {

    private var observeEventsJob: Job? = null

    fun initialize() {
        if (observeEventsJob?.isActive == true) return

        observeEventsJob = appScope.launch(ioDispatcher) {
            realtimeDataSource.incomingEvents.collect { event ->
                when (event) {
                    is WsServerEvent.NewMessage -> handleNewMessage(event)
                    is WsServerEvent.MessageDeleted -> handleMessageDeleted(event)
                    is WsServerEvent.ParticipantJoined -> handleParticipantJoined(event)
                    is WsServerEvent.ParticipantLeft -> handleParticipantLeft(event)
                    is WsServerEvent.Error -> {
                        // Ignore or log error
                    }
                }
            }
        }
    }

    private suspend fun handleNewMessage(event: WsServerEvent.NewMessage) {
        val dto = event.message
        chatDao.reconcileMessageAck(
            chatRoomId = dto.chatRoomId,
            messageId = dto.id,
            serverTimestamp = dto.createdAt.toEpochMilliseconds(),
            status = MessageDeliveryStatus.SENT
        )
    }

    private suspend fun handleMessageDeleted(event: WsServerEvent.MessageDeleted) {
        chatDao.updateDeliveryStatus(event.messageId, MessageDeliveryStatus.DELETED)
    }

    private suspend fun handleParticipantJoined(event: WsServerEvent.ParticipantJoined) {
        chatDao.handleParticipantJoined(
            chatRoomId = event.chatRoomId,
            newUsers = event.newUsers.map { it.toEntity() },
            addedBy = event.addedBy?.toEntity()
        )
    }

    private suspend fun handleParticipantLeft(event: WsServerEvent.ParticipantLeft) {
        chatDao.handleParticipantLeft(
            chatRoomId = event.chatRoomId,
            leftUser = event.leftUser.toEntity(),
            kickedBy = event.kickedBy?.toEntity()
        )
    }
}
