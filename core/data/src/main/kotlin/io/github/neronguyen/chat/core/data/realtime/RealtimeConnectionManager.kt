package io.github.neronguyen.chat.core.data.realtime

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.neronguyen.chat.core.common.network.AppScope
import io.github.neronguyen.chat.core.common.network.ChatDispatchers
import io.github.neronguyen.chat.core.common.network.Dispatcher
import io.github.neronguyen.chat.core.data.mapper.toEntity
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyen.chat.core.network.model.websocket.WsServerEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val realtimeDataSource: ChatRealtimeDataSource,
    private val chatDao: ChatDao,
    @AppScope private val appScope: CoroutineScope,
    @Dispatcher(ChatDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : DefaultLifecycleObserver {

    private var observeEventsJob: Job? = null
    private var connectionSupervisorJob: Job? = null
    private var disconnectJob: Job? = null

    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        disconnectJob?.cancel()
        disconnectJob = null
        startConnectionSupervision()
    }

    override fun onStop(owner: LifecycleOwner) {
        disconnectJob?.cancel()
        disconnectJob = appScope.launch(Dispatchers.Main.immediate) {
            delay(DISCONNECT_DELAY_MS.milliseconds)
            connectionSupervisorJob?.cancel()
            connectionSupervisorJob = null
            observeEventsJob?.cancel()
            observeEventsJob = null
            realtimeDataSource.disconnect()
        }
    }

    private fun startObservingEventsIfNeeded() {
        if (observeEventsJob?.isActive == true) {
            return
        }

        observeEventsJob = appScope.launch(ioDispatcher) {
            realtimeDataSource.incomingEvents.collect { event ->
                when (event) {
                    is WsServerEvent.NewMessage -> handleNewMessage(event)
                    else -> {
                        // TODO: Handle typing, member join/leave, deleted events
                    }
                }
            }
        }
    }

    private suspend fun handleNewMessage(event: WsServerEvent.NewMessage) {
        val dto = event.message
        chatDao.upsertMessages(listOf(dto.toEntity(MessageDeliveryStatus.SENT)))
        chatDao.reconcileMessageAck(
            chatRoomId = dto.chatRoomId,
            messageId = dto.id,
            serverTimestamp = dto.createdAt.toEpochMilliseconds(),
            status = MessageDeliveryStatus.SENT
        )
    }

    private fun startConnectionSupervision() {
        if (connectionSupervisorJob?.isActive == true) return

        connectionSupervisorJob = appScope.launch(Dispatchers.Main.immediate) {
            var retryAttempt = 0

            realtimeDataSource.connectionState.collectLatest { state ->
                when (state) {
                    is SocketConnectionState.Connected -> {
                        retryAttempt = 0
                        startObservingEventsIfNeeded()
                    }

                    is SocketConnectionState.Failed,
                    is SocketConnectionState.Disconnected -> {
                        if (retryAttempt == 0) {
                            realtimeDataSource.connect()
                            return@collectLatest
                        }

                        retryAttempt++
                        delay(calculateBackoffDelay(retryAttempt).milliseconds)
                        realtimeDataSource.connect()
                    }

                    is SocketConnectionState.Connecting,
                    is SocketConnectionState.Idle -> {
                        // Ignore
                    }
                }
            }
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelayMs = 1_000L
        val maxDelayMs = 30_000L

        // 2^attempt * baseDelay
        val expDelay = baseDelayMs * (1L shl attempt.coerceAtMost(5))
        val cappedDelay = expDelay.coerceAtMost(maxDelayMs)

        val jitter = (cappedDelay * 0.2 * Random.nextDouble(-1.0, 1.0)).toLong()
        return (cappedDelay + jitter).coerceAtLeast(baseDelayMs)
    }

    companion object {
        private const val DISCONNECT_DELAY_MS = 30_000L
    }
}
