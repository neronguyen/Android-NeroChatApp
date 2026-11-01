package io.github.neronguyen.chat.feature.auth.presentation.forgot

sealed interface ForgotPasswordUiEvent {
    data object Submit : ForgotPasswordUiEvent
}
