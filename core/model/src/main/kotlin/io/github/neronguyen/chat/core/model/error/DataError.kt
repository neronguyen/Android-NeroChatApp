package io.github.neronguyen.chat.core.model.error

import kotlin.time.Duration

sealed interface DataError {

    /** The request never received a server response. */
    sealed interface Connectivity : DataError {
        data object NoInternet : Connectivity
        data object Timeout : Connectivity
    }

    /** The server answered, but refused for a NON-BUSINESS reason. */
    sealed interface Remote : DataError {
        data object Unauthorized : Remote
        data object Forbidden : Remote
        data class RateLimited(val retryAfter: Duration? = null) : Remote
        data object Unavailable : Remote
    }

    /** Client and server disagree (contract drift, bug, malformed payload). */
    sealed interface Unexpected : DataError {
        data object Serialization : Unexpected
        data class UnknownCode(val status: Int, val rawCode: String?) : Unexpected
        data object RequestRejected : Unexpected
    }

    /** Device-side state problem. */
    sealed interface Local : DataError {
        data object NotAuthenticated : Local

        /** A message referenced by id (e.g. for retry) no longer exists in the local database. */
        data object MessageNotFound : Local
    }

    /** WebSocket channel problem. */
    sealed interface Realtime : DataError {
        data object NotConnected : Realtime
        data object SendFailed : Realtime
    }
}
