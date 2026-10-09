package io.github.neronguyen.chat.core.datastore

import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import kotlinx.coroutines.flow.Flow

interface TokenDataSource {
    val authState: Flow<AuthState>
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?

    suspend fun saveAuthData(user: User, accessToken: String, refreshToken: String)
    suspend fun saveTokens(accessToken: String, refreshToken: String)

    suspend fun clearAuthData()
}
