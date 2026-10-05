package io.github.neronguyen.chat.core.data.repository

import arrow.core.Either
import arrow.core.flatMap
import io.github.neronguyen.chat.core.data.mapper.toDomain
import io.github.neronguyen.chat.core.data.mapper.toDomainUser
import io.github.neronguyen.chat.core.datastore.TokenDataSource
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.AuthNetworkDataSource
import io.github.neronguyen.chat.core.network.model.LoginRequest
import io.github.neronguyen.chat.core.network.model.RefreshTokenRequest
import io.github.neronguyen.chat.core.network.model.RegisterRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val networkDataSource: AuthNetworkDataSource,
    private val tokenDataSource: TokenDataSource
) : AuthRepository {

    override fun getAuthState(): Flow<AuthState> = tokenDataSource.authState

    override suspend fun login(
        email: String,
        password: String
    ): Either<DataError.Network, User> {
        return networkDataSource.login(LoginRequest(email = email, password = password))
            .flatMap { response ->
                val user = response.toDomainUser()
                tokenDataSource.saveAuthData(user, response.accessToken, response.refreshToken)
                Either.Right(user)
            }
    }

    override suspend fun register(
        email: String,
        displayName: String,
        password: String
    ): Either<DataError.Network, User> {
        return networkDataSource.register(
            RegisterRequest(
                email = email,
                displayName = displayName,
                password = password
            )
        ).map { userDto -> userDto.toDomain() }
    }

    override suspend fun refreshToken(): Either<DataError.Network, User> {
        val currentRefreshToken = tokenDataSource.getRefreshToken()
            ?: return Either.Left(DataError.Network.Unknown)

        return networkDataSource.refreshToken(RefreshTokenRequest(currentRefreshToken))
            .flatMap { response ->
                val user = response.toDomainUser()
                tokenDataSource.saveAuthData(user, response.accessToken, response.refreshToken)
                Either.Right(user)
            }
            .onLeft {
                tokenDataSource.clearAuthData()
            }
    }

    override suspend fun logout(): Either<DataError.Network, Unit> {
        val currentRefreshToken = tokenDataSource.getRefreshToken()
        if (currentRefreshToken != null) {
            networkDataSource.logout(RefreshTokenRequest(currentRefreshToken))
        }
        tokenDataSource.clearAuthData()
        return Either.Right(Unit)
    }
}
