package io.github.neronguyen.chat.feature.auth.presentation.login

import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.feature.auth.presentation.util.AuthValidationError

data class LoginUiState(
    val isLoading: Boolean = false,
    val validationError: AuthValidationError? = null,
    val dataError: DataError? = null,
    val isSuccess: Boolean = false
)
