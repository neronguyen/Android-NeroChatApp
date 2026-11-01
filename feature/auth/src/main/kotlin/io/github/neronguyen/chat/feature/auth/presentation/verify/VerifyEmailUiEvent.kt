package io.github.neronguyen.chat.feature.auth.presentation.verify

sealed interface VerifyEmailUiEvent {
    data object Verify : VerifyEmailUiEvent
    data object ResendCode : VerifyEmailUiEvent
    data object DismissInfo : VerifyEmailUiEvent
}
