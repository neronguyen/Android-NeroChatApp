package io.github.neronguyen.chat.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.neronguyen.chat.core.model.error.DataError

@Composable
fun DataError.asUiText(): String {
    return when (this) {
        DataError.Network.NoInternet -> stringResource(R.string.error_no_internet)
        DataError.Network.RequestTimeout -> stringResource(R.string.error_timeout)
        DataError.Network.Serialization -> stringResource(R.string.error_serialization)
        DataError.Network.Unknown -> stringResource(R.string.error_unknown)
    }
}
