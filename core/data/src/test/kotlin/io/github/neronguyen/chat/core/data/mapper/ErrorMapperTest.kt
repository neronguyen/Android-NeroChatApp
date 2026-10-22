package io.github.neronguyen.chat.core.data.mapper

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.util.NetworkError
import io.github.neronguyenvn.nerochat.contract.auth.error.AuthServerError
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
    fun `NetworkError Http with rawCode maps to UnknownCode`() {
        val httpError = NetworkError.Http(status = 400, rawCode = "GATEWAY_ERROR")
        val dataError = httpError.toDataError()

        assertIs<DataError.Unexpected.UnknownCode>(dataError)
        assertEquals(400, dataError.status)
        assertEquals("GATEWAY_ERROR", dataError.rawCode)
    }

    @Test
    fun `NetworkError Transport mappers return appropriate Connectivity or Unexpected errors`() {
        assertEquals(DataError.Connectivity.NoInternet, NetworkError.Transport.NoInternet.toDataError())
        assertEquals(DataError.Connectivity.Timeout, NetworkError.Transport.Timeout.toDataError())
        assertEquals(DataError.Unexpected.Serialization, NetworkError.Transport.Serialization.toDataError())
    }

    @Test
    fun `NetworkError Nothing toDataError maps Transport and Http accurately`() {
        val transportNothing: NetworkError<Nothing> = NetworkError.Transport.NoInternet
        assertEquals(DataError.Connectivity.NoInternet, transportNothing.toDataError())

        val httpNothing: NetworkError<Nothing> = NetworkError.Http(status = 429, retryAfterSeconds = 45L)
        val dataError = httpNothing.toDataError()
        assertIs<DataError.Remote.RateLimited>(dataError)
        assertEquals(45.seconds, dataError.retryAfter)
    }

    @Test
    fun `NetworkError wildcard toTransportOrHttpDataError maps Server to RequestRejected`() {
        val serverError: NetworkError<*> = NetworkError.Server(
            error = AuthServerError.ValidationFailed,
            message = "Validation failed"
        )
        assertEquals(DataError.Unexpected.RequestRejected, serverError.toTransportOrHttpDataError())
    }

    @Test
    fun `Auth login domain mapping maps InvalidCredentials and EmailNotVerified correctly`() {
        val invalidCreds: NetworkError<AuthServerError.Login> = NetworkError.Server(
            error = AuthServerError.InvalidCredentials,
            message = "Invalid credentials"
        )
        assertEquals(AuthError.Login.InvalidCredentials, invalidCreds.toLoginError())

        val emailNotVerified: NetworkError<AuthServerError.Login> = NetworkError.Server(
            error = AuthServerError.EmailNotVerified,
            message = "Email not verified"
        )
        assertEquals(AuthError.Login.EmailNotVerified, emailNotVerified.toLoginError())
    }

    @Test
    fun `Auth login domain mapping maps RateLimitExceeded server error with retry duration`() {
        val rateLimited: NetworkError<AuthServerError.Login> = NetworkError.Server(
            error = AuthServerError.RateLimitExceeded,
            message = "Too many attempts",
            retryAfterSeconds = 120L
        )
        val domainError = rateLimited.toLoginError()

        assertIs<AuthError.Login.Data>(domainError)
        val innerData = domainError.error
        assertIs<DataError.Remote.RateLimited>(innerData)
        assertEquals(120.seconds, innerData.retryAfter)
    }

    @Test
    fun `Auth login domain mapping maps HTTP 429 fallback to RateLimited data error`() {
        val http429: NetworkError<AuthServerError.Login> = NetworkError.Http(status = 429, retryAfterSeconds = 30L)
        val domainError = http429.toLoginError()

        assertIs<AuthError.Login.Data>(domainError)
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
        assertEquals(AuthError.Register.EmailAlreadyInUse, userExists.toRegisterError())
    }

    @Test
    fun `Auth refreshToken toDataError maps InvalidToken to Unauthorized`() {
        val invalidToken: NetworkError<AuthServerError.RefreshToken> = NetworkError.Server(
            error = AuthServerError.InvalidToken,
            message = "Token invalid"
        )
        assertEquals(DataError.Remote.Unauthorized, invalidToken.toDataError())
    }
}
