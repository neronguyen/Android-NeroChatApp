package io.github.neronguyen.chat.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.neronguyen.chat.core.database.dao.ChatDao
import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.database.entity.ChatParticipantEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomParticipantCrossRef

@Database(
    entities = [
        ChatMessageEntity::class,
        ChatRoomEntity::class,
        ChatParticipantEntity::class,
        ChatRoomParticipantCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
