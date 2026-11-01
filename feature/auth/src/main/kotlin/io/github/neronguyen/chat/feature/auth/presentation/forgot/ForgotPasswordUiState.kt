package io.github.neronguyen.chat.feature.auth.presentation.forgot

import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyenvn.nerochat.contract.validation.auth.AuthValidationError

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val submittedEmail: String = "",
    val validationError: AuthValidationError.Email? = null,
    val authError: AuthError? = null,
)
