package io.github.neronguyen.chat.core.model.error

sealed interface ChatError {

    sealed interface FindParticipant : ChatError {
        data object ParticipantNotFound : FindParticipant
        data class Data(val error: DataError) : FindParticipant
    }

    sealed interface SyncRoom : ChatError {
        data object RoomNotFound : SyncRoom
        data class Data(val error: DataError) : SyncRoom
    }

    sealed interface CreateRoom : ChatError {
        data object ParticipantNotFound : CreateRoom
        data object InvalidRoomSize : CreateRoom
        data class Data(val error: DataError) : CreateRoom
    }

    sealed interface AddParticipants : ChatError {
        data object RoomNotFound : AddParticipants
        data object ParticipantNotFound : AddParticipants
        data object AlreadyInRoom : AddParticipants
        data class Data(val error: DataError) : AddParticipants
    }

    sealed interface LeaveRoom : ChatError {
        data object RoomNotFound : LeaveRoom
        data object ParticipantNotFound : LeaveRoom
        data class Data(val error: DataError) : LeaveRoom
    }

    sealed interface DeleteMessage : ChatError {
        data object MessageNotFound : DeleteMessage
        data class Data(val error: DataError) : DeleteMessage
    }
}
