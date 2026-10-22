package io.github.neronguyen.chat.feature.chat.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.neronguyen.chat.core.model.error.ChatError
import io.github.neronguyen.chat.core.ui.asUiText
import io.github.neronguyen.chat.feature.chat.R
import io.github.neronguyen.chat.feature.chat.presentation.conversation.ConversationActionError

@Composable
fun ConversationActionError.asUiText(): String = when (this) {
    is ConversationActionError.Send -> error.asUiText()
    is ConversationActionError.Delete -> error.asUiText()
    is ConversationActionError.Leave -> error.asUiText()
}

@Composable
fun ChatError.FindParticipant.asUiText(): String = when (this) {
    ChatError.FindParticipant.ParticipantNotFound -> stringResource(R.string.chat_error_participant_not_found)
    is ChatError.FindParticipant.Data -> error.asUiText()
}

@Composable
fun ChatError.SyncRoom.asUiText(): String = when (this) {
    ChatError.SyncRoom.RoomNotFound -> stringResource(R.string.chat_error_room_not_found)
    is ChatError.SyncRoom.Data -> error.asUiText()
}

@Composable
fun ChatError.CreateRoom.asUiText(): String = when (this) {
    ChatError.CreateRoom.ParticipantNotFound -> stringResource(R.string.chat_error_participant_not_found)
    ChatError.CreateRoom.InvalidRoomSize -> stringResource(R.string.chat_error_invalid_room_size)
    is ChatError.CreateRoom.Data -> error.asUiText()
}

@Composable
fun ChatError.AddParticipants.asUiText(): String = when (this) {
    ChatError.AddParticipants.ParticipantNotFound -> stringResource(R.string.chat_error_participant_not_found)
    ChatError.AddParticipants.RoomNotFound -> stringResource(R.string.chat_error_room_not_found)
    ChatError.AddParticipants.AlreadyInRoom -> stringResource(R.string.chat_error_already_in_room)
    is ChatError.AddParticipants.Data -> error.asUiText()
}

@Composable
fun ChatError.LeaveRoom.asUiText(): String = when (this) {
    ChatError.LeaveRoom.ParticipantNotFound -> stringResource(R.string.chat_error_participant_not_found)
    ChatError.LeaveRoom.RoomNotFound -> stringResource(R.string.chat_error_room_not_found)
    is ChatError.LeaveRoom.Data -> error.asUiText()
}

@Composable
fun ChatError.DeleteMessage.asUiText(): String = when (this) {
    ChatError.DeleteMessage.MessageNotFound -> stringResource(R.string.chat_error_message_not_found)
    is ChatError.DeleteMessage.Data -> error.asUiText()
}
