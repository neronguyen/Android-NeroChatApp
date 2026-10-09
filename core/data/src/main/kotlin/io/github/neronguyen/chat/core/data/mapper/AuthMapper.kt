package io.github.neronguyen.chat.core.data.mapper

import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.network.model.AuthenticatedUserDto
import io.github.neronguyen.chat.core.network.model.UserDto

internal fun UserDto.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified
    )
}

internal fun AuthenticatedUserDto.toDomainUser(): User {
    return user.toDomain()
}
