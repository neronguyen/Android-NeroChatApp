package io.github.neronguyen.chat.feature.auth.presentation.login

sealed interface LoginUiEvent {
    data object Login : LoginUiEvent
}
