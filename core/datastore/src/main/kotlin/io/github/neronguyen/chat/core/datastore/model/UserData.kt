package io.github.neronguyen.chat.core.datastore.model

import io.github.neronguyen.chat.core.model.User
import kotlinx.serialization.Serializable

@Serializable
internal data class UserData(
    val userId: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean
)

internal fun UserData.toDomain(): User = User(
    id = userId,
    email = email,
    displayName = displayName,
    isEmailVerified = isEmailVerified
)
