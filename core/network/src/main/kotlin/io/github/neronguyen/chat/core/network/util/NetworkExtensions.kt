package io.github.neronguyen.chat.core.network.util

import arrow.core.Either
import io.github.neronguyenvn.nerochat.contract.error.ApiErrorResponse
import io.github.neronguyenvn.nerochat.contract.error.ServerError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

internal suspend fun <T, S : ServerError> safeCallInternal(
    json: Json,
    isUnit: Boolean,
    errorMapper: (String) -> S?,
    execute: suspend () -> Response<T>,
): Either<NetworkError<S>, T> {
    val response = try {
        execute()
    } catch (_: SerializationException) {
        return Either.Left(NetworkError.Transport.Serialization)
    } catch (e: IOException) {
        return Either.Left(e.toTransportError())
    } catch (_: UnresolvedAddressException) {
        return Either.Left(NetworkError.Transport.NoInternet)
    }

    val retryAfterSeconds = response.headers()["Retry-After"]?.toLongOrNull()

    if (response.isSuccessful) {
        val body = response.body()
        return when {
            body != null -> Either.Right(body)
            isUnit -> Either.Right(Unit as T)
            else -> Either.Left(
                NetworkError.Http(
                    status = response.code(),
                    rawCode = null,
                    retryAfterSeconds = retryAfterSeconds,
                )
            )
        }
    }

    val errorBodyString = try {
        response.errorBody()?.string()
    } catch (_: IOException) {
        null
    }

    if (!errorBodyString.isNullOrBlank()) {
        val apiError = try {
            json.decodeFromString<ApiErrorResponse>(errorBodyString)
        } catch (_: Exception) {
            null
        }

        if (apiError != null) {
            val effectiveRetryAfter = apiError.retryAfterSeconds ?: retryAfterSeconds
            val serverError = errorMapper(apiError.code)
            if (serverError != null) {
                return Either.Left(
                    NetworkError.Server(
                        error = serverError,
                        message = apiError.message,
                        fieldErrors = apiError.fieldErrors,
                        retryAfterSeconds = effectiveRetryAfter,
                    )
                )
            }
            return Either.Left(
                NetworkError.Http(
                    status = response.code(),
                    rawCode = apiError.code,
                    retryAfterSeconds = effectiveRetryAfter,
                )
            )
        }
    }

    return Either.Left(
        NetworkError.Http(
            status = response.code(),
            rawCode = null,
            retryAfterSeconds = retryAfterSeconds,
        )
    )
}

internal suspend inline fun <reified T, S : ServerError> safeCall(
    json: Json,
    noinline errorMapper: (String) -> S?,
    noinline execute: suspend () -> Response<T>,
): Either<NetworkError<S>, T> = safeCallInternal(
    json = json,
    isUnit = T::class == Unit::class,
    errorMapper = errorMapper,
    execute = execute,
)

internal fun IOException.toTransportError(): NetworkError.Transport = when (this) {
    is SocketTimeoutException -> NetworkError.Transport.Timeout
    is UnknownHostException,
    is ConnectException -> NetworkError.Transport.NoInternet
    else -> NetworkError.Transport.NoInternet
}
