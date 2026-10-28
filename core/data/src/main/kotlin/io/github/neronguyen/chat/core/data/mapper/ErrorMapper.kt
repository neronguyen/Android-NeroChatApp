package io.github.neronguyen.chat.core.data.mapper

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.model.error.ChatError
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.util.NetworkError
import io.github.neronguyenvn.nerochat.contract.auth.error.AuthServerError
import io.github.neronguyenvn.nerochat.contract.chat.error.ChatServerError
import io.github.neronguyenvn.nerochat.contract.error.CommonServerError
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
    else -> DataError.Unexpected.UnknownCode(status)
}

internal fun NetworkError.Common.toDataError(): DataError = when (error) {
    CommonServerError.Unauthorized -> DataError.Remote.Unauthorized
    CommonServerError.RateLimitExceeded -> DataError.Remote.RateLimited(retryAfterSeconds?.seconds)
    CommonServerError.BadRequest,
    CommonServerError.ValidationFailed -> DataError.Unexpected.RequestRejected(fieldErrors)
    CommonServerError.InternalServerError -> DataError.Remote.Unavailable
}

@JvmName("toNothingDataError")
internal fun NetworkError<Nothing>.toDataError(): DataError = when (this) {
    is NetworkError.Common -> toDataError()
    is NetworkError.Transport -> toDataError()
    is NetworkError.Http -> toDataError()
    is NetworkError.Server -> error("Unreachable: NetworkError<Nothing> cannot have Server error")
}

/** Generic splitter: callers only describe the endpoint-specific server branch. */
internal inline fun <S : ServerError, D> NetworkError<S>.toDomain(
    wrap: (DataError) -> D,
    onServer: (NetworkError.Server<S>) -> D,
): D = when (this) {
    is NetworkError.Server -> onServer(this)
    is NetworkError.Common -> wrap(toDataError())
    is NetworkError.Transport -> wrap(toDataError())
    is NetworkError.Http -> wrap(toDataError())
}

internal fun NetworkError.Server<*>.rateLimited() =
    DataError.Remote.RateLimited(retryAfterSeconds?.seconds)

// Auth mappers
internal fun NetworkError<AuthServerError.Login>.toLoginError(): AuthError.Login =
    toDomain(wrap = AuthError::Data) { server ->
        when (server.error) {
            AuthServerError.InvalidCredentials -> AuthError.InvalidCredentials
            AuthServerError.EmailNotVerified -> AuthError.EmailNotVerified
        }
    }

internal fun NetworkError<AuthServerError.Register>.toRegisterError(): AuthError.Register =
    toDomain(wrap = AuthError::Data) { server ->
        when (server.error) {
            AuthServerError.UserAlreadyExists -> AuthError.EmailAlreadyInUse
        }
    }

@JvmName("toRefreshTokenDataError")
internal fun NetworkError<AuthServerError.RefreshToken>.toDataError(): DataError =
    toDomain(wrap = { it }) { server ->
        when (server.error) {
            AuthServerError.InvalidToken,
            AuthServerError.UserNotFound -> DataError.Remote.Unauthorized
        }
    }

@JvmName("toLogoutDataError")
internal fun NetworkError<AuthServerError.Logout>.toDataError(): DataError =
    toDomain(wrap = { it }) { server ->
        when (server.error) {
            AuthServerError.InvalidToken -> DataError.Remote.Unauthorized
        }
    }

// Chat mappers
@JvmName("toGetMessagesDataError")
internal fun NetworkError<ChatServerError.GetMessages>.toDataError(): DataError =
    toDomain(wrap = { it }) { server ->
        when (server.error) {
            ChatServerError.ParticipantNotInRoom -> DataError.Remote.Forbidden
        }
    }

internal fun NetworkError<ChatServerError.GetParticipant>.toFindParticipantError(): ChatError.FindParticipant =
    toDomain(wrap = ChatError::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.ParticipantNotFound
        }
    }

internal fun NetworkError<ChatServerError.GetRoom>.toSyncRoomError(): ChatError.SyncRoom =
    toDomain(wrap = ChatError::Data) { server ->
        when (server.error) {
            ChatServerError.ChatRoomNotFound -> ChatError.RoomNotFound
        }
    }

internal fun NetworkError<ChatServerError.CreateRoom>.toCreateRoomError(): ChatError.CreateRoom =
    toDomain(wrap = ChatError::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.ParticipantNotFound
            ChatServerError.InvalidChatRoomSize -> ChatError.InvalidRoomSize
        }
    }

internal fun NetworkError<ChatServerError.AddParticipants>.toAddParticipantsError(): ChatError.AddParticipants =
    toDomain(wrap = ChatError::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.ParticipantNotFound
            ChatServerError.ChatRoomNotFound -> ChatError.RoomNotFound
            ChatServerError.ParticipantAlreadyInRoom -> ChatError.AlreadyInRoom
            ChatServerError.ParticipantNotInRoom -> ChatError.ParticipantNotInRoom
        }
    }

internal fun NetworkError<ChatServerError.LeaveRoom>.toLeaveRoomError(): ChatError.LeaveRoom =
    toDomain(wrap = ChatError::Data) { server ->
        when (server.error) {
            ChatServerError.ChatParticipantNotFound -> ChatError.ParticipantNotFound
            ChatServerError.ChatRoomNotFound -> ChatError.RoomNotFound
        }
    }

internal fun NetworkError<ChatServerError.DeleteMessage>.toDeleteMessageError(): ChatError.DeleteMessage =
    toDomain(wrap = ChatError::Data) { server ->
        when (server.error) {
            ChatServerError.MessageNotFound -> ChatError.MessageNotFound
            ChatServerError.NotMessageSender -> ChatError.NotMessageSender
        }
    }
