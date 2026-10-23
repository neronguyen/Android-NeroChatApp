package io.github.neronguyen.chat.core.data.mapper

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.model.error.ChatError
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.util.NetworkError
import io.github.neronguyenvn.nerochat.contract.auth.error.AuthServerError
import io.github.neronguyenvn.nerochat.contract.chat.error.ChatServerError
import io.github.neronguyenvn.nerochat.contract.error.ServerError
import kotlin.time.Duration.Companion.seconds

/**
 * Every endpoint has its own contract sub-interface (even when it only carries `UNAUTHORIZED`),
 * so there is deliberately no wildcard `NetworkError<*>.toDataError()`: each endpoint gets an
 * exhaustive mapper below and a new contract error stops compiling until it is handled.
 */
internal fun NetworkError.Transport.toDataError(): DataError = when (this) {
    NetworkError.Transport.NoInternet -> DataError.Connectivity.NoInternet
    NetworkError.Transport.Timeout -> DataError.Connectivity.Timeout
    NetworkError.Transport.Serialization -> DataError.Unexpected.Serialization
}

internal fun NetworkError.Http.toDataError(): DataError = when {
    status == 429 -> DataError.Remote.RateLimited(retryAfterSeconds?.seconds)
    status == 401 -> DataError.Remote.Unauthorized
    status == 403 -> DataError.Remote.Forbidden
    status in 500..599 -> DataError.Remote.Unavailable
    else -> DataError.Unexpected.UnknownCode(status, rawCode)
}

/** Generic splitter: callers only describe the endpoint-specific server branch. */
internal inline fun <S : ServerError, D> NetworkError<S>.toDomain(
    wrap: (DataError) -> D,
    onServer: (NetworkError.Server<S>) -> D,
): D = when (this) {
    is NetworkError.Server -> onServer(this)
    is NetworkError.Transport -> wrap(toDataError())
    is NetworkError.Http -> wrap(toDataError())
}

internal fun NetworkError.Server<*>.rateLimited() =
    DataError.Remote.RateLimited(retryAfterSeconds?.seconds)

// Auth mappers
internal fun NetworkError<AuthServerError.Login>.toLoginError(): AuthError.Login =
    toDomain(wrap = AuthError.Login::Data) { server ->
        when (server.error) {
            AuthServerError.InvalidCredentials -> AuthError.Login.InvalidCredentials
            AuthServerError.EmailNotVerified -> AuthError.Login.EmailNotVerified
            AuthServerError.RateLimitExceeded -> AuthError.Login.Data(server.rateLimited())
            AuthServerError.ValidationFailed -> AuthError.Login.Data(DataError.Unexpected.RequestRejected)
        }
    }

internal fun NetworkError<AuthServerError.Register>.toRegisterError(): AuthError.Register =
    toDomain(wrap = AuthError.Register::Data) { server ->
        when (server.error) {
            AuthServerError.UserAlreadyExists -> AuthError.Register.EmailAlreadyInUse
            AuthServerError.RateLimitExceeded -> AuthError.Register.Data(server.rateLimited())
            AuthServerError.ValidationFailed -> AuthError.Register.Data(DataError.Unexpected.RequestRejected)
        }
    }

@JvmName("toRefreshTokenDataError")
internal fun NetworkError<AuthServerError.RefreshToken>.toDataError(): DataError = when (this) {
    is NetworkError.Server -> when (this.error) {
        AuthServerError.InvalidToken,
        AuthServerError.UserNotFound -> DataError.Remote.Unauthorized
        AuthServerError.RateLimitExceeded -> rateLimited()
        AuthServerError.ValidationFailed -> DataError.Unexpected.RequestRejected
    }
    is NetworkError.Transport -> toDataError()
    is NetworkError.Http -> toDataError()
}

@JvmName("toLogoutDataError")
internal fun NetworkError<AuthServerError.Logout>.toDataError(): DataError = when (this) {
    is NetworkError.Server -> when (this.error) {
        AuthServerError.InvalidToken -> DataError.Remote.Unauthorized
        AuthServerError.ValidationFailed -> DataError.Unexpected.RequestRejected
    }
    is NetworkError.Transport -> toDataError()
    is NetworkError.Http -> toDataError()
}

// Chat mappers
@JvmName("toGetRoomsDataError")
internal fun NetworkError<ChatServerError.GetRooms>.toDataError(): DataError = when (this) {
    is NetworkError.Server -> when (this.error) {
        ChatServerError.Unauthorized -> DataError.Remote.Unauthorized
    }
    is NetworkError.Transport -> toDataError()
    is NetworkError.Http -> toDataError()
}
@JvmName("toGetMessagesDataError")
internal fun NetworkError<ChatServerError.GetMessages>.toDataError(): DataError = when (this) {
    is NetworkError.Server -> when (this.error) {
        ChatServerError.Forbidden -> DataError.Remote.Forbidden
        ChatServerError.Unauthorized -> DataError.Remote.Unauthorized
        ChatServerError.BadRequest -> DataError.Unexpected.RequestRejected
    }
    is NetworkError.Transport -> toDataError()
    is NetworkError.Http -> toDataError()
}
internal fun NetworkError<ChatServerError.GetParticipant>.toFindParticipantError(): ChatError.FindParticipant =
    toDomain(wrap = ChatError.FindParticipant::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.FindParticipant.ParticipantNotFound
            ChatServerError.Unauthorized -> ChatError.FindParticipant.Data(DataError.Remote.Unauthorized)
        }
    }

internal fun NetworkError<ChatServerError.GetRoom>.toSyncRoomError(): ChatError.SyncRoom =
    toDomain(wrap = ChatError.SyncRoom::Data) { server ->
        when (server.error) {
            ChatServerError.ChatRoomNotFound -> ChatError.SyncRoom.RoomNotFound
            ChatServerError.Unauthorized -> ChatError.SyncRoom.Data(DataError.Remote.Unauthorized)
            ChatServerError.BadRequest -> ChatError.SyncRoom.Data(DataError.Unexpected.RequestRejected)
        }
    }

internal fun NetworkError<ChatServerError.CreateRoom>.toCreateRoomError(): ChatError.CreateRoom =
    toDomain(wrap = ChatError.CreateRoom::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.CreateRoom.ParticipantNotFound
            ChatServerError.InvalidChatRoomSize -> ChatError.CreateRoom.InvalidRoomSize
            ChatServerError.ValidationFailed -> ChatError.CreateRoom.Data(DataError.Unexpected.RequestRejected)
            ChatServerError.Unauthorized -> ChatError.CreateRoom.Data(DataError.Remote.Unauthorized)
        }
    }

internal fun NetworkError<ChatServerError.AddParticipants>.toAddParticipantsError(): ChatError.AddParticipants =
    toDomain(wrap = ChatError.AddParticipants::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.AddParticipants.ParticipantNotFound
            ChatServerError.ChatRoomNotFound -> ChatError.AddParticipants.RoomNotFound
            ChatServerError.ParticipantAlreadyInRoom -> ChatError.AddParticipants.AlreadyInRoom
            ChatServerError.Forbidden -> ChatError.AddParticipants.Data(DataError.Remote.Forbidden)
            ChatServerError.ValidationFailed -> ChatError.AddParticipants.Data(DataError.Unexpected.RequestRejected)
            ChatServerError.Unauthorized -> ChatError.AddParticipants.Data(DataError.Remote.Unauthorized)
            ChatServerError.BadRequest -> ChatError.AddParticipants.Data(DataError.Unexpected.RequestRejected)
        }
    }

internal fun NetworkError<ChatServerError.LeaveRoom>.toLeaveRoomError(): ChatError.LeaveRoom =
    toDomain(wrap = ChatError.LeaveRoom::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.LeaveRoom.ParticipantNotFound
            ChatServerError.ChatRoomNotFound -> ChatError.LeaveRoom.RoomNotFound
            ChatServerError.Unauthorized -> ChatError.LeaveRoom.Data(DataError.Remote.Unauthorized)
            ChatServerError.BadRequest -> ChatError.LeaveRoom.Data(DataError.Unexpected.RequestRejected)
        }
    }

internal fun NetworkError<ChatServerError.DeleteMessage>.toDeleteMessageError(): ChatError.DeleteMessage =
    toDomain(wrap = ChatError.DeleteMessage::Data) { server ->
        when (server.error) {
            ChatServerError.MessageNotFound -> ChatError.DeleteMessage.MessageNotFound
            ChatServerError.Forbidden -> ChatError.DeleteMessage.Data(DataError.Remote.Forbidden)
            ChatServerError.Unauthorized -> ChatError.DeleteMessage.Data(DataError.Remote.Unauthorized)
            ChatServerError.BadRequest -> ChatError.DeleteMessage.Data(DataError.Unexpected.RequestRejected)
        }
    }
