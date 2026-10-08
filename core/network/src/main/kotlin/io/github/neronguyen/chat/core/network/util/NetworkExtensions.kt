package io.github.neronguyen.chat.core.network.util

import arrow.core.raise.Raise
import io.github.neronguyen.chat.core.model.error.DataError
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

// TODO: Handle based on response status code
context(raise: Raise<DataError.Network>)
internal suspend inline fun <reified T> safeCall(
    execute: suspend () -> Response<T>
): T {
    try {
        val response = execute()
        if (!response.isSuccessful) {
            raise.raise(DataError.Network.Unknown)
        }

        return response.body() ?: raise.raise(DataError.Network.Unknown)

    } catch (_: SerializationException) {
        raise.raise(DataError.Network.Serialization)

    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        raise.raise(e.asNetworkError())
    }
}

internal fun Exception.asNetworkError(): DataError.Network = when (this) {
    is UnknownHostException,
    is UnresolvedAddressException,
    is ConnectException -> DataError.Network.NoInternet

    is SocketTimeoutException -> DataError.Network.RequestTimeout

    else -> DataError.Network.Unknown
}
