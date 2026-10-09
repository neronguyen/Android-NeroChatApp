package io.github.neronguyen.chat.feature.auth.presentation.register

sealed interface RegisterUiEvent {
    data object Register : RegisterUiEvent
}
