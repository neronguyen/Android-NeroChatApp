package io.github.neronguyen.astrocommander.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Placeholder(
    val id: Long,
    val title: String,
    val completed: Boolean
)
