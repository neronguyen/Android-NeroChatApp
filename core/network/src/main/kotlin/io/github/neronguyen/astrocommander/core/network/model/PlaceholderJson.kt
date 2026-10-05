package io.github.neronguyen.astrocommander.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceholderJson(
    val id: Long,
    val title: String,
    val completed: Boolean
)
