package io.github.neronguyen.astrocommander.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.neronguyen.astrocommander.core.database.model.PlaceholderJsonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceholderJsonDao {

    @Upsert
    suspend fun upsertPlaceholderJson(placeholderJson: PlaceholderJsonEntity)

    @Query("SELECT * FROM PlaceholderJsonEntity")
    fun observePlaceholderJsonList(): Flow<List<PlaceholderJsonEntity>>

    @Query("SELECT * FROM PlaceholderJsonEntity WHERE id = :id")
    suspend fun getPlaceholderJson(id: Long): PlaceholderJsonEntity?
}
