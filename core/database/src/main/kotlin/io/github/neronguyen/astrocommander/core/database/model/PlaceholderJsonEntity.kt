package io.github.neronguyen.astrocommander.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlaceholderJsonEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val title: String,
    val completed: Boolean
)
