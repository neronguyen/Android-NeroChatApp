package io.github.neronguyen.chat.feature.auth.presentation.reset

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyenvn.nerochat.contract.validation.auth.AuthValidationError

data class ResetPasswordUiState(
    val email: String,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isResending: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val infoMessage: String? = null,
    val validationError: AuthValidationError.ResetPassword? = null,
    val authError: AuthError.ResetPassword? = null,
)
