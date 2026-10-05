package io.github.neronguyen.astrocommander

import androidx.navigation3.runtime.NavKey
import io.github.neronguyen.astrocommander.core.model.Placeholder
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Home : Route

    @Serializable
    data class Detail(val item: Placeholder) : Route
}
