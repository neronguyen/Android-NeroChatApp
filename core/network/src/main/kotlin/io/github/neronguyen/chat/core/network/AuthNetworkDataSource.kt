package io.github.neronguyen.chat.core.network

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.core.network.model.AuthenticatedUserDto
import io.github.neronguyen.chat.core.network.model.LoginRequest
import io.github.neronguyen.chat.core.network.model.RefreshTokenRequest
import io.github.neronguyen.chat.core.network.model.RegisterRequest
import io.github.neronguyen.chat.core.network.model.UserDto

interface AuthNetworkDataSource {
    suspend fun register(request: RegisterRequest): Either<DataError.Network, UserDto>
    suspend fun login(request: LoginRequest): Either<DataError.Network, AuthenticatedUserDto>
    suspend fun refreshToken(request: RefreshTokenRequest): Either<DataError.Network, AuthenticatedUserDto>
    suspend fun logout(request: RefreshTokenRequest): Either<DataError.Network, Unit>
}
