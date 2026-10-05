package io.github.neronguyen.chat.feature.auth.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.neronguyen.chat.feature.auth.R

enum class AuthValidationError {
    EmptyEmail,
    InvalidEmail,
    EmptyPassword,
    EmptyDisplayName
}

@Composable
fun AuthValidationError.asUiText(): String {
    return when (this) {
        AuthValidationError.EmptyEmail -> stringResource(R.string.error_empty_email)
        AuthValidationError.InvalidEmail -> stringResource(R.string.error_invalid_email)
        AuthValidationError.EmptyPassword -> stringResource(R.string.error_empty_password)
        AuthValidationError.EmptyDisplayName -> stringResource(R.string.error_empty_display_name)
    }
}
