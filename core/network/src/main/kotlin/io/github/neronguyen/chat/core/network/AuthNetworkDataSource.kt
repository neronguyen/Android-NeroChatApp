package io.github.neronguyen.chat.core.network

import arrow.core.Either
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyenvn.nerochat.contract.auth.AuthenticatedUserDto
import io.github.neronguyenvn.nerochat.contract.auth.LoginRequest
import io.github.neronguyenvn.nerochat.contract.auth.RefreshTokenRequest
import io.github.neronguyenvn.nerochat.contract.auth.RegisterRequest
import io.github.neronguyenvn.nerochat.contract.auth.UserDto

interface AuthNetworkDataSource {
    suspend fun register(request: RegisterRequest): Either<DataError.Network, UserDto>
    suspend fun login(request: LoginRequest): Either<DataError.Network, AuthenticatedUserDto>
    suspend fun refreshToken(request: RefreshTokenRequest): Either<DataError.Network, AuthenticatedUserDto>
    suspend fun logout(request: RefreshTokenRequest): Either<DataError.Network, Unit>
}
