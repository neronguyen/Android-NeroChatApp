package io.github.neronguyen.chat.core.network.websocket

import arrow.core.Either
import io.github.neronguyen.chat.core.datastore.TokenDataSource
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.BuildConfig
import io.github.neronguyen.chat.core.network.ChatRealtimeDataSource
import io.github.neronguyen.chat.core.network.di.AuthenticatedOkHttp
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState.Connected
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState.Connecting
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState.Disconnected
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState.Failed
import io.github.neronguyen.chat.core.network.model.websocket.WsClientMessage
import io.github.neronguyen.chat.core.network.model.websocket.WsServerEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

@OptIn(ExperimentalAtomicApi::class)
@Singleton
internal class ChatWebSocketClient @Inject constructor(
    @AuthenticatedOkHttp private val okHttpClient: OkHttpClient,
    private val tokenDataSource: TokenDataSource,
) : ChatRealtimeDataSource {

    private val json = Json {
        classDiscriminator = EVENT_TYPE
        ignoreUnknownKeys = true
    }

    private val connectMutex = Mutex()
    private var webSocket = AtomicReference<WebSocket?>(null)

    private val _connectionState =
        MutableStateFlow<SocketConnectionState>(Disconnected)
    override val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<WsServerEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incomingEvents: Flow<WsServerEvent> = _incomingEvents.asSharedFlow()

    override suspend fun connect() = connectMutex.withLock {
        if (webSocket.load() != null) {
            return@withLock
        }

        if (_connectionState.value is Connected || _connectionState.value is Connecting) {
            return@withLock
        }

        _connectionState.update { Connecting }

        val token = tokenDataSource.getAccessToken()
        if (token.isNullOrEmpty()) {
            _connectionState.update {
                Failed(IllegalStateException("No auth token available"))
            }
            return@withLock
        }

        val rawBaseUrl = BuildConfig.BASE_URL.ifEmpty { "http://10.0.2.2:8080/" }
        val wsBaseUrl = when {
            rawBaseUrl.startsWith("https://") -> rawBaseUrl.replaceFirst("https://", "wss://")
            rawBaseUrl.startsWith("http://") -> rawBaseUrl.replaceFirst("http://", "ws://")
            else -> rawBaseUrl
        }.removeSuffix("/")

        val url = "$wsBaseUrl/ws/chat"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket.update { okHttpClient.newWebSocket(request, createListener()) }
    }

    override suspend fun sendMessage(
        message: WsClientMessage
    ): Either<DataError.Network, Unit> {
        val socket = webSocket.load() ?: return Either.Left(DataError.Network.SocketError)
        if (_connectionState.value !is Connected) {
            return Either.Left(DataError.Network.SocketError)
        }

        return try {
            val text = json.encodeToString(message)
            val success = socket.send(text)
            if (success) {
                Either.Right(Unit)
            } else {
                Either.Left(DataError.Network.SocketError)
            }
        } catch (_: Exception) {
            Either.Left(DataError.Network.SocketError)
        }
    }

    override suspend fun disconnect() = connectMutex.withLock {
        webSocket.load()?.close(1000, "User disconnected")
        webSocket.update { null }
        _connectionState.update { Disconnected }
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.update { Connected }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val event = json.decodeFromString<WsServerEvent>(text)
                _incomingEvents.tryEmit(event)
            } catch (_: Exception) {
                // Ignore unparseable frames
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            this@ChatWebSocketClient.webSocket.update { null }
            _connectionState.update { Failed(t) }
            _incomingEvents.tryEmit(
                WsServerEvent.Error(
                    "CONNECTION_ERROR",
                    t.message ?: "WebSocket connection error"
                )
            )
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            this@ChatWebSocketClient.webSocket.update { null }
            _connectionState.update { Disconnected }
        }
    }

    companion object {
        private const val EVENT_TYPE = "type"
    }
}
