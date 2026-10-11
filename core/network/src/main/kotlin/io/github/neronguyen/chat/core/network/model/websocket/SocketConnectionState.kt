package io.github.neronguyen.chat.core.network.model.websocket

sealed interface SocketConnectionState {
    data object Disconnected : SocketConnectionState
    data object Connecting : SocketConnectionState
    data object Connected : SocketConnectionState
    data class Failed(val throwable: Throwable? = null) : SocketConnectionState
}
