package io.github.neronguyen.chat.core.network

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.model.websocket.SocketConnectionState
import io.github.neronguyenvn.nerochat.contract.chat.IncomingWsMessage
import io.github.neronguyenvn.nerochat.contract.chat.OutgoingWsMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRealtimeDataSource {
    val incomingEvents: Flow<OutgoingWsMessage>
    val connectionState: StateFlow<SocketConnectionState>

    suspend fun connect()
    suspend fun disconnect()

    suspend fun sendMessage(
        message: IncomingWsMessage
    ): Either<DataError.Realtime, Unit>
}
