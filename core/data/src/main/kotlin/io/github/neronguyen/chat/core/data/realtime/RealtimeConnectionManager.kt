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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalAtomicApi::class)
@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val realtimeDataSource: ChatRealtimeDataSource,
    @AppScope private val appScope: CoroutineScope,
    @Dispatcher(ChatDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : DefaultLifecycleObserver {

    private val isForeground = MutableStateFlow(false)
    private val initialized = AtomicBoolean(false)

    fun initialize() {
        if (!initialized.compareAndSet(expectedValue = false, newValue = true)) return

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        appScope.launch(ioDispatcher) {
            isForeground.collectLatest { foreground ->
                if (foreground) {
                    superviseConnection()
                } else {
                    delay(DISCONNECT_DELAY_MS.milliseconds)
                    realtimeDataSource.disconnect()
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground.value = false
    }

    private suspend fun superviseConnection() {
        var retryAttempt = 0

        realtimeDataSource.connectionState.collect { state ->
            when (state) {
                is SocketConnectionState.Connected -> {
                    retryAttempt = 0
                }

                is SocketConnectionState.Failed,
                is SocketConnectionState.Disconnected -> {
                    if (retryAttempt > 0) {
                        delay(calculateBackoffDelay(retryAttempt).milliseconds)
                    }

                    retryAttempt++
                    realtimeDataSource.connect()
                }

                is SocketConnectionState.Connecting -> Unit
            }
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelayMs = 1_000L
        val maxDelayMs = 30_000L
        val expDelay = baseDelayMs * (1L shl attempt.coerceAtMost(5))
        val cappedDelay = expDelay.coerceAtMost(maxDelayMs)
        val jitter = (cappedDelay * 0.2 * Random.nextDouble(-1.0, 1.0)).toLong()
        return (cappedDelay + jitter).coerceAtLeast(baseDelayMs)
    }

    companion object {
        private const val DISCONNECT_DELAY_MS = 30_000L
    }
}
