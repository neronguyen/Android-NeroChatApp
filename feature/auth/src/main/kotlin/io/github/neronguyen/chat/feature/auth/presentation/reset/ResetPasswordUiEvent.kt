package io.github.neronguyen.chat.feature.auth.presentation.reset

sealed interface ResetPasswordUiEvent {
    data object Reset : ResetPasswordUiEvent
    data object ResendCode : ResetPasswordUiEvent
    data object DismissInfo : ResetPasswordUiEvent
}
