package io.github.neronguyen.chat.core.data.realtime

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.neronguyen.chat.core.common.network.AppScope
import io.github.neronguyen.chat.core.common.network.ChatDispatchers
import io.github.neronguyen.chat.core.common.network.Dispatcher
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
    @AppScope private val appScope: CoroutineScope,
    @Dispatcher(ChatDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : DefaultLifecycleObserver {

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
        disconnectJob = appScope.launch(ioDispatcher) {
            delay(DISCONNECT_DELAY_MS.milliseconds)
            connectionSupervisorJob?.cancel()
            connectionSupervisorJob = null
            realtimeDataSource.disconnect()
        }
    }

    private fun startConnectionSupervision() {
        if (connectionSupervisorJob?.isActive == true) return

        connectionSupervisorJob = appScope.launch(ioDispatcher) {
            var retryAttempt = 0

            realtimeDataSource.connectionState.collectLatest { state ->
                when (state) {
                    is SocketConnectionState.Connected -> {
                        retryAttempt = 0
                    }

                    is SocketConnectionState.Failed,
                    is SocketConnectionState.Disconnected -> {
                        if (retryAttempt == 0) {
                            retryAttempt++
                            realtimeDataSource.connect()
                            return@collectLatest
                        }

                        retryAttempt++
                        delay(calculateBackoffDelay(retryAttempt).milliseconds)
                        realtimeDataSource.connect()
                    }

                    is SocketConnectionState.Connecting -> {
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
