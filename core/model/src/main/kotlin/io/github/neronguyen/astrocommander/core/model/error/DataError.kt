package io.github.neronguyen.astrocommander.core.model.error

sealed interface DataError {

    enum class Network : DataError {
        NoInternet,
        RequestTimeout,
        Serialization,
        Unknown
    }
}
