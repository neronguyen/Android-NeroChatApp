package io.github.neronguyen.chat.core.data.mapper

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.model.error.ChatError
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.util.NetworkError
import io.github.neronguyenvn.nerochat.contract.auth.error.AuthServerError
import io.github.neronguyenvn.nerochat.contract.chat.error.ChatServerError
import io.github.neronguyenvn.nerochat.contract.error.CommonServerError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class ErrorMapperTest {

    @Test
    fun `NetworkError Http 429 maps to RateLimited with duration`() {
        val httpError = NetworkError.Http(status = 429, retryAfterSeconds = 60L)
        val dataError = httpError.toDataError()

        assertIs<DataError.Remote.RateLimited>(dataError)
        assertEquals(60.seconds, dataError.retryAfter)
    }

    @Test
    fun `NetworkError Http 429 without Retry-After maps to RateLimited with null duration`() {
        val httpError = NetworkError.Http(status = 429, retryAfterSeconds = null)
        val dataError = httpError.toDataError()

        assertIs<DataError.Remote.RateLimited>(dataError)
        assertNull(dataError.retryAfter)
    }

    @Test
    fun `NetworkError Http 401 maps to Unauthorized`() {
        val httpError = NetworkError.Http(status = 401)
        assertEquals(DataError.Remote.Unauthorized, httpError.toDataError())
    }

    @Test
    fun `NetworkError Http 403 maps to Forbidden`() {
        val httpError = NetworkError.Http(status = 403)
        assertEquals(DataError.Remote.Forbidden, httpError.toDataError())
    }

    @Test
    fun `NetworkError Http 500 and 503 map to Unavailable`() {
        assertEquals(DataError.Remote.Unavailable, NetworkError.Http(status = 500).toDataError())
        assertEquals(DataError.Remote.Unavailable, NetworkError.Http(status = 503).toDataError())
    }

    @Test
    fun `NetworkError Http maps to UnknownCode`() {
        val httpError = NetworkError.Http(status = 400)
        val dataError = httpError.toDataError()

        assertIs<DataError.Unexpected.UnknownCode>(dataError)
        assertEquals(400, dataError.status)
    }

    @Test
    fun `NetworkError Common maps to appropriate DataErrors`() {
        assertEquals(
            DataError.Remote.Unauthorized,
            NetworkError.Common(CommonServerError.Unauthorized, "Unauthorized").toDataError()
        )
        assertEquals(
            DataError.Remote.RateLimited(60.seconds),
            NetworkError.Common(
                CommonServerError.RateLimitExceeded,
                "Rate limit exceeded",
                retryAfterSeconds = 60L
            ).toDataError()
        )
        assertEquals(
            DataError.Unexpected.RequestRejected,
            NetworkError.Common(CommonServerError.BadRequest, "Bad request").toDataError()
        )
        assertEquals(
            DataError.Unexpected.RequestRejected,
            NetworkError.Common(CommonServerError.ValidationFailed, "Validation failed").toDataError()
        )
        assertEquals(
            DataError.Remote.Unavailable,
            NetworkError.Common(CommonServerError.InternalServerError, "Server error").toDataError()
        )
    }

    @Test
    fun `NetworkError Transport mappers return appropriate Connectivity or Unexpected errors`() {
        assertEquals(DataError.Connectivity.NoInternet, NetworkError.Transport.NoInternet.toDataError())
        assertEquals(DataError.Connectivity.Timeout, NetworkError.Transport.Timeout.toDataError())
        assertEquals(DataError.Unexpected.Serialization, NetworkError.Transport.Serialization.toDataError())
    }

    @Test
    fun `NetworkError with Nothing toDataError maps Common, Http 429 and Transport`() {
        val unauthorized: NetworkError<Nothing> = NetworkError.Common(
            error = CommonServerError.Unauthorized,
            message = "Unauthorized"
        )
        assertEquals(DataError.Remote.Unauthorized, unauthorized.toDataError())

        val gateway429: NetworkError<Nothing> =
            NetworkError.Http(status = 429, retryAfterSeconds = 45L)
        val rateLimited = gateway429.toDataError()
        assertIs<DataError.Remote.RateLimited>(rateLimited)
        assertEquals(45.seconds, rateLimited.retryAfter)

        val offline: NetworkError<Nothing> = NetworkError.Transport.NoInternet
        assertEquals(DataError.Connectivity.NoInternet, offline.toDataError())
    }

    @Test
    fun `Auth login domain mapping maps InvalidCredentials and EmailNotVerified correctly`() {
        val invalidCreds: NetworkError<AuthServerError.Login> = NetworkError.Server(
            error = AuthServerError.InvalidCredentials,
            message = "Invalid credentials"
        )
        assertEquals(AuthError.InvalidCredentials, invalidCreds.toLoginError())

        val emailNotVerified: NetworkError<AuthServerError.Login> = NetworkError.Server(
            error = AuthServerError.EmailNotVerified,
            message = "Email not verified"
        )
        assertEquals(AuthError.EmailNotVerified, emailNotVerified.toLoginError())
    }

    @Test
    fun `Auth login domain mapping maps Common RateLimitExceeded with retry duration`() {
        val rateLimited: NetworkError<AuthServerError.Login> = NetworkError.Common(
            error = CommonServerError.RateLimitExceeded,
            message = "Too many attempts",
            retryAfterSeconds = 120L
        )
        val domainError = rateLimited.toLoginError()

        assertIs<AuthError.Data>(domainError)
        val innerData = domainError.error
        assertIs<DataError.Remote.RateLimited>(innerData)
        assertEquals(120.seconds, innerData.retryAfter)
    }

    @Test
    fun `Auth login domain mapping maps HTTP 429 fallback to RateLimited data error`() {
        val http429: NetworkError<AuthServerError.Login> = NetworkError.Http(status = 429, retryAfterSeconds = 30L)
        val domainError = http429.toLoginError()

        assertIs<AuthError.Data>(domainError)
        val innerData = domainError.error
        assertIs<DataError.Remote.RateLimited>(innerData)
        assertEquals(30.seconds, innerData.retryAfter)
    }

    @Test
    fun `Auth register domain mapping maps UserAlreadyExists to EmailAlreadyInUse`() {
        val userExists: NetworkError<AuthServerError.Register> = NetworkError.Server(
            error = AuthServerError.UserAlreadyExists,
            message = "User already exists"
        )
        assertEquals(AuthError.EmailAlreadyInUse, userExists.toRegisterError())
    }

    @Test
    fun `Auth refreshToken toDataError maps InvalidToken to Unauthorized`() {
        val invalidToken: NetworkError<AuthServerError.RefreshToken> = NetworkError.Server(
            error = AuthServerError.InvalidToken,
            message = "Token invalid"
        )
        assertEquals(DataError.Remote.Unauthorized, invalidToken.toDataError())
    }

    @Test
    fun `Auth logout toDataError maps InvalidToken to Unauthorized`() {
        val invalidToken: NetworkError<AuthServerError.Logout> = NetworkError.Server(
            error = AuthServerError.InvalidToken,
            message = "Token invalid"
        )
        assertEquals(DataError.Remote.Unauthorized, invalidToken.toDataError())
    }

    @Test
    fun `Chat getMessages toDataError maps ParticipantNotInRoom to Forbidden`() {
        val participantNotInRoom: NetworkError<ChatServerError.GetMessages> = NetworkError.Server(
            error = ChatServerError.ParticipantNotInRoom,
            message = "Not in room"
        )
        assertEquals(DataError.Remote.Forbidden, participantNotInRoom.toDataError())
    }

    @Test
    fun `Chat addParticipants toAddParticipantsError maps domain errors`() {
        val notInRoom: NetworkError<ChatServerError.AddParticipants> = NetworkError.Server(
            error = ChatServerError.ParticipantNotInRoom,
            message = "Not in room"
        )
        assertEquals(ChatError.ParticipantNotInRoom, notInRoom.toAddParticipantsError())

        val alreadyInRoom: NetworkError<ChatServerError.AddParticipants> = NetworkError.Server(
            error = ChatServerError.ParticipantAlreadyInRoom,
            message = "Already in room"
        )
        assertEquals(ChatError.AlreadyInRoom, alreadyInRoom.toAddParticipantsError())
    }

    @Test
    fun `Chat deleteMessage toDeleteMessageError maps MessageNotFound and NotMessageSender`() {
        val notFound: NetworkError<ChatServerError.DeleteMessage> = NetworkError.Server(
            error = ChatServerError.MessageNotFound,
            message = "Not found"
        )
        assertEquals(ChatError.MessageNotFound, notFound.toDeleteMessageError())

        val notSender: NetworkError<ChatServerError.DeleteMessage> = NetworkError.Server(
            error = ChatServerError.NotMessageSender,
            message = "Not sender"
        )
        assertEquals(ChatError.NotMessageSender, notSender.toDeleteMessageError())
    }
}
