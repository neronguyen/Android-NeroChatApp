package io.github.neronguyen.chat.core.network

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyenvn.nerochat.contract.chat.WsClientMessage
import io.github.neronguyenvn.nerochat.contract.chat.WsServerEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRealtimeDataSource {
    val incomingEvents: Flow<WsServerEvent>
    val connectionState: StateFlow<SocketConnectionState>

    suspend fun connect()
    suspend fun disconnect()

    suspend fun sendMessage(
        message: WsClientMessage
    ): Either<DataError.Realtime, Unit>
}
