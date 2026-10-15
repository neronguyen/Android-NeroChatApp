package io.github.neronguyen.chat.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.database.entity.ChatParticipantEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomParticipantCrossRef
import io.github.neronguyen.chat.core.database.model.ChatRoomWithDetails
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // --- Queries ---

    @Transaction
    @Query("SELECT * FROM chat_rooms ORDER BY lastActivityAt DESC")
    fun getChatsWithDetails(): Flow<List<ChatRoomWithDetails>>

    @Transaction
    @Query("SELECT * FROM chat_rooms WHERE id = :roomId")
    fun getChatWithDetails(roomId: String): Flow<ChatRoomWithDetails?>

    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(roomId: String, limit: Int = 20): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_room_participants WHERE roomId IN (:roomIds)")
    suspend fun getCrossRefsForRooms(roomIds: List<String>): List<ChatRoomParticipantCrossRef>

    // --- Upserts ---

    @Upsert
    suspend fun upsertRoom(room: ChatRoomEntity)

    @Upsert
    suspend fun upsertRooms(rooms: List<ChatRoomEntity>)

    @Upsert
    suspend fun upsertParticipant(participant: ChatParticipantEntity)

    @Upsert
    suspend fun upsertParticipants(participants: List<ChatParticipantEntity>)

    @Upsert
    suspend fun upsertCrossRefs(crossRefs: List<ChatRoomParticipantCrossRef>)

    @Upsert
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Upsert
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    // --- Updates & Deletes ---

    @Query("UPDATE chat_messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: MessageDeliveryStatus)

    @Query(
        """
        UPDATE chat_messages 
        SET deliveryStatus = :failedStatus 
        WHERE deliveryStatus = :sendingStatus
        """
    )
    suspend fun markSendingMessagesAsFailed(
        sendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENDING,
        failedStatus: MessageDeliveryStatus = MessageDeliveryStatus.FAILED
    )

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chat_room_participants WHERE roomId = :roomId AND userId = :userId")
    suspend fun deleteParticipantCrossRef(roomId: String, userId: String)

    @Query("UPDATE chat_rooms SET lastMessageId = :messageId, lastActivityAt = :timestamp WHERE id = :roomId")
    suspend fun updateRoomLastActivity(roomId: String, messageId: String, timestamp: Long)

    @Delete
    suspend fun deleteCrossRefs(crossRefs: List<ChatRoomParticipantCrossRef>)

    // --- Transactions ---

    @Transaction
    suspend fun insertSendingMessage(message: ChatMessageEntity) {
        upsertMessage(message)
        updateRoomLastActivity(
            roomId = message.roomId,
            messageId = message.id,
            timestamp = message.timestamp
        )
    }

    @Transaction
    suspend fun upsertIncomingOrAckedMessage(message: ChatMessageEntity) {
        upsertMessage(message)
        updateRoomLastActivity(
            roomId = message.roomId,
            messageId = message.id,
            timestamp = message.timestamp
        )
    }

    @Transaction
    suspend fun insertFullChat(
        room: ChatRoomEntity,
        creator: ChatParticipantEntity,
        participants: List<ChatParticipantEntity>,
        lastMessage: ChatMessageEntity?
    ) {
        upsertParticipant(creator)
        upsertParticipants(participants)
        lastMessage?.let { upsertMessage(it) }
        upsertRoom(room)

        val allParticipantIds = (participants.map { it.userId } + creator.userId).distinct()
        val crossRefs = allParticipantIds.map { userId ->
            ChatRoomParticipantCrossRef(roomId = room.id, userId = userId)
        }

        upsertCrossRefs(crossRefs)
    }

    @Transaction
    suspend fun insertFullChats(
        rooms: List<ChatRoomEntity>,
        participants: List<ChatParticipantEntity>,
        lastMessages: List<ChatMessageEntity>,
        crossRefs: List<ChatRoomParticipantCrossRef>
    ) {
        if (rooms.isEmpty()) return

        upsertParticipants(participants)
        upsertMessages(lastMessages)
        upsertRooms(rooms)

        val roomIds = rooms.map { it.id }
        val existingCrossRefs = getCrossRefsForRooms(roomIds)
        val newCrossRefSet = crossRefs.toSet()

        val staleCrossRefs = existingCrossRefs.filter { it !in newCrossRefSet }
        if (staleCrossRefs.isNotEmpty()) {
            deleteCrossRefs(staleCrossRefs)
        }

        upsertCrossRefs(crossRefs)
    }

    @Transaction
    suspend fun handleParticipantJoined(
        roomId: String,
        newUsers: List<ChatParticipantEntity>,
        addedBy: ChatParticipantEntity? = null
    ) {
        addedBy?.let { upsertParticipant(it) }
        if (newUsers.isNotEmpty()) {
            upsertParticipants(newUsers)
            val crossRefs = newUsers.map { participant ->
                ChatRoomParticipantCrossRef(roomId = roomId, userId = participant.userId)
            }
            upsertCrossRefs(crossRefs)
        }
    }

    @Transaction
    suspend fun handleParticipantLeft(
        roomId: String,
        leftUser: ChatParticipantEntity,
        kickedBy: ChatParticipantEntity? = null
    ) {
        kickedBy?.let { upsertParticipant(it) }
        deleteParticipantCrossRef(roomId = roomId, userId = leftUser.userId)
    }
}
