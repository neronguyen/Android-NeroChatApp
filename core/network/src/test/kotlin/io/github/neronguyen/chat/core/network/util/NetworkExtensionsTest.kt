package io.github.neronguyen.chat.core.network.util

import arrow.core.Either
import io.github.neronguyenvn.nerochat.contract.auth.error.AuthServerError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkExtensionsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun <T> createErrorResponse(
        code: Int,
        body: String = "",
        headers: Map<String, String> = emptyMap(),
        mediaType: String = "application/json",
    ): Response<T> {
        val rawBuilder = okhttp3.Response.Builder()
            .code(code)
            .message("Error $code")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://api.example.com").build())
            .body(body.toResponseBody(mediaType.toMediaType()))

        headers.forEach { (name, value) ->
            rawBuilder.header(name, value)
        }

        return Response.error(body.toResponseBody(mediaType.toMediaType()), rawBuilder.build())
    }

    @Test
    fun `safeCall parses successful body`() = runTest {
        val result: Either<NetworkError<AuthServerError>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) },
            execute = { Response.success("success_data") }
        )

        assertIs<Either.Right<String>>(result)
        assertEquals("success_data", result.value)
    }

    @Test
    fun `safeCall handles Unit return type on 204 No Content`() = runTest {
        val result: Either<NetworkError<AuthServerError>, Unit> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) },
            execute = { Response.success<Unit>(204, null) }
        )

        assertIs<Either.Right<Unit>>(result)
        assertEquals(Unit, result.value)
    }

    @Test
    fun `safeCall maps HTTP 429 with Retry-After header to NetworkError Http with retryAfterSeconds`() = runTest {
        val response = createErrorResponse<String>(
            code = 429,
            body = "Too many requests",
            headers = mapOf("Retry-After" to "60"),
            mediaType = "text/plain"
        )

        val result: Either<NetworkError<AuthServerError>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) },
            execute = { response }
        )

        assertIs<Either.Left<NetworkError.Http>>(result)
        val error = result.value
        assertEquals(429, error.status)
        assertNull(error.rawCode)
        assertEquals(60L, error.retryAfterSeconds)
    }

    @Test
    fun `safeCall maps recognized contract error to NetworkError Server with retryAfterSeconds`() = runTest {
        val errorBody = """
            {
                "code": "RATE_LIMIT_EXCEEDED",
                "message": "Too many login attempts",
                "fieldErrors": {}
            }
        """.trimIndent()

        val response = createErrorResponse<String>(
            code = 429,
            body = errorBody,
            headers = mapOf("Retry-After" to "120")
        )

        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { response }
        )

        assertIs<Either.Left<NetworkError.Server<AuthServerError.Login>>>(result)
        val serverError = result.value
        assertEquals(AuthServerError.RateLimitExceeded, serverError.error)
        assertEquals("Too many login attempts", serverError.message)
        assertEquals(120L, serverError.retryAfterSeconds)
        assertTrue(serverError.fieldErrors.isEmpty())
    }

    @Test
    fun `safeCall extracts retryAfterSeconds from ApiErrorResponse body when header is absent`() = runTest {
        val errorBody = """
            {
                "code": "RATE_LIMIT_EXCEEDED",
                "message": "Too many requests",
                "fieldErrors": {},
                "retryAfterSeconds": 45
            }
        """.trimIndent()

        val response = createErrorResponse<String>(
            code = 429,
            body = errorBody,
        )

        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { response }
        )

        assertIs<Either.Left<NetworkError.Server<AuthServerError.Login>>>(result)
        val serverError = result.value
        assertEquals(AuthServerError.RateLimitExceeded, serverError.error)
        assertEquals(45L, serverError.retryAfterSeconds)
    }

    @Test
    fun `safeCall prioritizes retryAfterSeconds from ApiErrorResponse body over header`() = runTest {
        val errorBody = """
            {
                "code": "RATE_LIMIT_EXCEEDED",
                "message": "Too many requests",
                "fieldErrors": {},
                "retryAfterSeconds": 30
            }
        """.trimIndent()

        val response = createErrorResponse<String>(
            code = 429,
            body = errorBody,
            headers = mapOf("Retry-After" to "60")
        )

        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { response }
        )

        assertIs<Either.Left<NetworkError.Server<AuthServerError.Login>>>(result)
        val serverError = result.value
        assertEquals(AuthServerError.RateLimitExceeded, serverError.error)
        assertEquals(30L, serverError.retryAfterSeconds)
    }

    @Test
    fun `safeCall maps unrecognized contract error to NetworkError Http with rawCode`() = runTest {
        val errorBody = """
            {
                "code": "CUSTOM_GATEWAY_ERROR",
                "message": "Something unusual happened",
                "fieldErrors": {}
            }
        """.trimIndent()

        val response = createErrorResponse<String>(code = 400, body = errorBody)

        val result: Either<NetworkError<AuthServerError>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) },
            execute = { response }
        )

        assertIs<Either.Left<NetworkError.Http>>(result)
        val error = result.value
        assertEquals(400, error.status)
        assertEquals("CUSTOM_GATEWAY_ERROR", error.rawCode)
        assertNull(error.retryAfterSeconds)
    }

    @Test
    fun `safeCall maps non-JSON error to NetworkError Http with null rawCode`() = runTest {
        val response = createErrorResponse<String>(
            code = 502,
            body = "<html>Bad Gateway</html>",
            mediaType = "text/html"
        )

        val result: Either<NetworkError<AuthServerError>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) },
            execute = { response }
        )

        assertIs<Either.Left<NetworkError.Http>>(result)
        val error = result.value
        assertEquals(502, error.status)
        assertNull(error.rawCode)
    }

    @Test
    fun `safeCall maps SocketTimeoutException to NetworkError Transport Timeout`() = runTest {
        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { throw SocketTimeoutException("Read timed out") }
        )

        assertIs<Either.Left<NetworkError.Transport>>(result)
        assertEquals(NetworkError.Transport.Timeout, result.value)
    }

    @Test
    fun `safeCall maps UnknownHostException to NetworkError Transport NoInternet`() = runTest {
        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { throw UnknownHostException("Unable to resolve host") }
        )

        assertIs<Either.Left<NetworkError.Transport>>(result)
        assertEquals(NetworkError.Transport.NoInternet, result.value)
    }

    @Test
    fun `safeCall maps ConnectException to NetworkError Transport NoInternet`() = runTest {
        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { throw ConnectException("Connection refused") }
        )

        assertIs<Either.Left<NetworkError.Transport>>(result)
        assertEquals(NetworkError.Transport.NoInternet, result.value)
    }

    @Test
    fun `safeCall maps UnresolvedAddressException to NetworkError Transport NoInternet`() = runTest {
        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { throw UnresolvedAddressException() }
        )

        assertIs<Either.Left<NetworkError.Transport>>(result)
        assertEquals(NetworkError.Transport.NoInternet, result.value)
    }

    @Test
    fun `safeCall maps SerializationException to NetworkError Transport Serialization`() = runTest {
        val result: Either<NetworkError<AuthServerError.Login>, String> = safeCall(
            json = json,
            errorMapper = { AuthServerError.fromCode(it) as? AuthServerError.Login },
            execute = { throw SerializationException("Malformed JSON") }
        )

        assertIs<Either.Left<NetworkError.Transport>>(result)
        assertEquals(NetworkError.Transport.Serialization, result.value)
    }

    @Test
    fun `safeCall rethrows IllegalStateException without swallowing`() = runTest {
        assertFailsWith<IllegalStateException> {
            safeCall<String, AuthServerError.Login>(json = json, errorMapper = { null }) {
                throw IllegalStateException("Programmer bug in response processing")
            }
        }
    }

    @Test
    fun `safeCall rethrows NullPointerException without swallowing`() = runTest {
        assertFailsWith<NullPointerException> {
            safeCall<String, AuthServerError.Login>(json = json, errorMapper = { null }) {
                throw NullPointerException("Null pointer encountered")
            }
        }
    }

    @Test
    fun `safeCall does not swallow CancellationException`() = runTest {
        assertFailsWith<CancellationException> {
            safeCall<String, AuthServerError.Login>(json = json, errorMapper = { null }) {
                throw CancellationException("Coroutine was cancelled")
            }
        }
    }
}
