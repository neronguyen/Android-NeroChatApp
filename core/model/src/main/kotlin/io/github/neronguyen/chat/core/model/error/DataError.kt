package io.github.neronguyen.chat.core.model.error

sealed interface DataError {

    enum class Network : DataError {
        NoInternet,
        RequestTimeout,
        Serialization,
        SocketError,
        Unknown
    }

    enum class Local : DataError {
        Unauthorized,
    }
}
