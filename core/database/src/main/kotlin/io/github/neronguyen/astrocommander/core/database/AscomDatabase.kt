package io.github.neronguyen.astrocommander.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.neronguyen.astrocommander.core.database.dao.PlaceholderJsonDao
import io.github.neronguyen.astrocommander.core.database.model.PlaceholderJsonEntity

@Database(
    entities = [
        PlaceholderJsonEntity::class
    ],
    version = 1,
    exportSchema = true

)
internal abstract class AscomDatabase : RoomDatabase() {

    abstract fun placeholderJsonDao(): PlaceholderJsonDao
}
