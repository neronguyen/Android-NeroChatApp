package io.github.neronguyen.chat.core.network

import arrow.core.Either
import arrow.core.raise.either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.api.RetrofitAuthNetworkApi
import io.github.neronguyen.chat.core.network.model.AuthenticatedUserDto
import io.github.neronguyen.chat.core.network.model.LoginRequest
import io.github.neronguyen.chat.core.network.model.RefreshTokenRequest
import io.github.neronguyen.chat.core.network.model.RegisterRequest
import io.github.neronguyen.chat.core.network.model.UserDto
import io.github.neronguyen.chat.core.network.util.safeCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RetrofitAuthNetwork @Inject constructor(
    private val api: RetrofitAuthNetworkApi
) : AuthNetworkDataSource {

    override suspend fun register(request: RegisterRequest): Either<DataError.Network, UserDto> =
        either { safeCall { api.register(request) } }

    override suspend fun login(request: LoginRequest): Either<DataError.Network, AuthenticatedUserDto> =
        either { safeCall { api.login(request) } }

    override suspend fun refreshToken(request: RefreshTokenRequest): Either<DataError.Network, AuthenticatedUserDto> =
        either { safeCall { api.refreshToken(request) } }

    override suspend fun logout(request: RefreshTokenRequest): Either<DataError.Network, Unit> =
        either { safeCall { api.logout(request) } }
}
