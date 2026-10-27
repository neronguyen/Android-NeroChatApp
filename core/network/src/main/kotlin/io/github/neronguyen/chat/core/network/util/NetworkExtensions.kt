package io.github.neronguyen.chat.core.network.util

import arrow.core.Either
import io.github.neronguyenvn.nerochat.contract.error.ApiErrorResponse
import io.github.neronguyenvn.nerochat.contract.error.CommonServerError
import io.github.neronguyenvn.nerochat.contract.error.ServerError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

internal suspend fun <T, S : ServerError> safeCallInternal(
    json: Json,
    isUnit: Boolean,
    errorSerializer: KSerializer<ApiErrorResponse<S>>,
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
        val serverError = try {
            json.decodeFromString(errorSerializer, errorBodyString)
        } catch (_: Exception) {
            null
        }

        if (serverError != null) {
            val effectiveRetryAfter = serverError.retryAfterSeconds ?: retryAfterSeconds
            return Either.Left(
                NetworkError.Server(
                    error = serverError.error,
                    message = serverError.message,
                    fieldErrors = serverError.fieldErrors,
                    retryAfterSeconds = effectiveRetryAfter,
                )
            )
        }

        val commonError = try {
            json.decodeFromString<ApiErrorResponse<CommonServerError>>(errorBodyString)
        } catch (_: Exception) {
            null
        }

        if (commonError != null) {
            val effectiveRetryAfter = commonError.retryAfterSeconds ?: retryAfterSeconds
            return Either.Left(
                NetworkError.Common(
                    error = commonError.error,
                    message = commonError.message,
                    fieldErrors = commonError.fieldErrors,
                    retryAfterSeconds = effectiveRetryAfter,
                )
            )
        }
    }

    return Either.Left(
        NetworkError.Http(
            status = response.code(),
            retryAfterSeconds = retryAfterSeconds,
        )
    )
}

internal suspend inline fun <reified T, reified S : ServerError> safeCall(
    json: Json,
    noinline execute: suspend () -> Response<T>,
): Either<NetworkError<S>, T> = safeCallInternal(
    json = json,
    isUnit = T::class == Unit::class,
    errorSerializer = serializer<ApiErrorResponse<S>>(),
    execute = execute,
)

internal fun IOException.toTransportError(): NetworkError.Transport = when (this) {
    is SocketTimeoutException -> NetworkError.Transport.Timeout
    is UnknownHostException,
    is ConnectException -> NetworkError.Transport.NoInternet
    else -> NetworkError.Transport.NoInternet
}
