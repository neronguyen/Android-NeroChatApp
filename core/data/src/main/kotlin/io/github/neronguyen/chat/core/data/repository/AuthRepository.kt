package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.DataError
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun login(email: String, password: String): Either<DataError.Network, User>
    suspend fun register(
        email: String,
        displayName: String,
        password: String
    ): Either<DataError.Network, User>

    // Return new access token
    suspend fun refreshToken(staleToken: String): Either<DataError.Network, String>
    suspend fun logout(): Either<DataError.Network, Unit>
}
