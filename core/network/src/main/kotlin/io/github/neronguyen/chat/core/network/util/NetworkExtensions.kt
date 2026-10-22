package io.github.neronguyen.chat.core.network.util

import arrow.core.Either
import io.github.neronguyenvn.nerochat.contract.error.ApiErrorResponse
import io.github.neronguyenvn.nerochat.contract.error.ServerError
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

internal suspend inline fun <reified T, S : ServerError> safeCall(
    json: Json,
    noinline errorMapper: (String) -> S?,
    execute: suspend () -> Response<T>,
): Either<NetworkError<S>, T> {
    try {
        val response = execute()
        if (response.isSuccessful) {
            val body = response.body()
            return when {
                body != null -> Either.Right(body)
                T::class == Unit::class -> Either.Right(Unit as T)
                else -> Either.Left(NetworkError.Http(response.code(), rawCode = null))
            }
        }

        val retryAfterSeconds = response.headers()["Retry-After"]?.toLongOrNull()
        val errorBodyString = response.errorBody()?.string()

        if (!errorBodyString.isNullOrBlank()) {
            val apiError = try {
                json.decodeFromString<ApiErrorResponse>(errorBodyString)
            } catch (_: Exception) {
                null
            }

            if (apiError != null) {
                val serverError = errorMapper(apiError.code)
                if (serverError != null) {
                    return Either.Left(
                        NetworkError.Server(
                            error = serverError,
                            message = apiError.message,
                            fieldErrors = apiError.fieldErrors,
                            retryAfterSeconds = retryAfterSeconds,
                        )
                    )
                }
                return Either.Left(NetworkError.Http(response.code(), rawCode = apiError.code))
            }
        }

        return Either.Left(NetworkError.Http(response.code(), rawCode = null))

    } catch (_: SerializationException) {
        return Either.Left(NetworkError.Transport.Serialization)
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        return Either.Left(e.toTransportError())
    }
}

internal suspend inline fun <reified T> safeCall(
    json: Json,
    noinline execute: suspend () -> Response<T>,
): Either<NetworkError<Nothing>, T> = safeCall(
    json = json,
    errorMapper = { null },
    execute = execute
)

internal fun Exception.toTransportError(): NetworkError.Transport = when (this) {
    is SocketTimeoutException -> NetworkError.Transport.Timeout
    is UnknownHostException,
    is UnresolvedAddressException,
    is ConnectException,
    is IOException -> NetworkError.Transport.NoInternet
    else -> NetworkError.Transport.NoInternet
}
