package io.github.neronguyen.chat.core.network.util

import io.github.neronguyenvn.nerochat.contract.error.ServerError

sealed interface NetworkError<out S : ServerError> {
    enum class Transport : NetworkError<Nothing> {
        NoInternet,
        Timeout,
        Serialization,
    }

    /** Non-2xx response that does not match the contract (e.g. 502 HTML page, unknown code). */
    data class Http(val status: Int, val rawCode: String?) : NetworkError<Nothing>

    /** Non-2xx response whose ApiErrorResponse.code matched the endpoint's contract error. */
    data class Server<out S : ServerError>(
        val error: S,
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap(),
        val retryAfterSeconds: Long? = null,
    ) : NetworkError<S>
}
