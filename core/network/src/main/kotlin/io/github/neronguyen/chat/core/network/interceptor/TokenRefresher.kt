package io.github.neronguyen.chat.core.network.interceptor

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError

fun interface TokenRefresher {
    suspend fun refresh(staleToken: String): Either<DataError, String>
}
