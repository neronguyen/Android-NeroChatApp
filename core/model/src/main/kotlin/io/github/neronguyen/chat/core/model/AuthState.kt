package io.github.neronguyen.chat.core.model

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: User, val accessToken: String) : AuthState
}
