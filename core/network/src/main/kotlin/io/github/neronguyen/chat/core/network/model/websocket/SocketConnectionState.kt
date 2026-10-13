package io.github.neronguyen.chat.core.network.model.websocket

sealed interface SocketConnectionState {
    data object Idle : SocketConnectionState
    data object Connecting : SocketConnectionState
    data object Connected : SocketConnectionState
    data object Disconnected : SocketConnectionState
    data class Failed(val throwable: Throwable? = null) : SocketConnectionState
}
