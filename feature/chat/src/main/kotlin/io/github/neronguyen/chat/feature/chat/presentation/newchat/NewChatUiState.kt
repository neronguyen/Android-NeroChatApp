package io.github.neronguyen.chat.feature.chat.presentation.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import io.github.neronguyen.chat.core.model.ChatParticipant
import io.github.neronguyen.chat.core.model.error.ChatError
import io.github.neronguyen.chat.core.model.error.DataError
import io.github.neronguyen.chat.feature.chat.R
import io.github.neronguyen.chat.feature.chat.presentation.list.FALLBACK_UNKNOWN_PARTICIPANT
import io.github.neronguyen.chat.feature.chat.presentation.list.deriveInitials
import io.github.neronguyenvn.nerochat.contract.validation.chat.ChatValidationError

enum class NewChatMode {
    DIRECT,
    GROUP,
}

@Composable
internal fun ChatValidationError.asUiText(): String = when (this) {
    ChatValidationError.EmptySearchEmail -> stringResource(R.string.new_chat_error_empty_email)
    ChatValidationError.InvalidSearchEmail -> stringResource(R.string.new_chat_error_invalid_email)
    ChatValidationError.SelfParticipantNotAllowed -> stringResource(R.string.new_chat_error_self_participant)
    ChatValidationError.MissingDirectRecipient,
    ChatValidationError.InvalidTargetUserId -> stringResource(R.string.new_chat_error_missing_direct_recipient)
    ChatValidationError.EmptyFirstMessage -> stringResource(R.string.new_chat_error_empty_first_message)
    ChatValidationError.TooFewGroupParticipants,
    ChatValidationError.InvalidGroupParticipantId -> stringResource(R.string.new_chat_error_no_group_participants)
    ChatValidationError.EmptyParticipantIds -> stringResource(R.string.new_chat_error_empty_participant_ids)
    ChatValidationError.InvalidParticipantId -> stringResource(R.string.new_chat_error_invalid_participant_id)
}

@Immutable
data class ParticipantUiModel(
    val userId: String,
    val email: String,
    val displayName: String,
    val initials: String,
    val avatarUrl: String?,
)

internal fun ChatParticipant.toParticipantUiModel(): ParticipantUiModel {
    val cleanEmail = email.trim()
    val resolvedName = displayName.trim().takeIf { it.isNotEmpty() }
        ?: cleanEmail.takeIf { it.isNotEmpty() }
        ?: FALLBACK_UNKNOWN_PARTICIPANT

    return ParticipantUiModel(
        userId = userId,
        email = cleanEmail,
        displayName = resolvedName,
        initials = deriveInitials(resolvedName),
        avatarUrl = profilePictureUrl?.trim()?.takeIf { it.isNotEmpty() },
    )
}

@Immutable
data class NewChatUiState(
    val mode: NewChatMode = NewChatMode.DIRECT,
    val isSearching: Boolean = false,
    val searchedParticipant: ParticipantUiModel? = null,
    val searchError: ChatError.FindParticipant? = null,
    val validationError: ChatValidationError? = null,
    val selectedDirectParticipant: ParticipantUiModel? = null,
    val selectedGroupParticipants: List<ParticipantUiModel> = emptyList(),
    val firstMessage: String = "",
    val groupName: String = "",
    val isCreatingRoom: Boolean = false,
    val createError: ChatError.CreateRoom? = null,
    val createdRoomId: String? = null,
) {
    val isSearchedParticipantSelected: Boolean
        get() {
            val searchedId = searchedParticipant?.userId ?: return false
            return when (mode) {
                NewChatMode.DIRECT -> selectedDirectParticipant?.userId == searchedId
                NewChatMode.GROUP -> selectedGroupParticipants.any { it.userId == searchedId }
            }
        }

    val canSubmitDirect: Boolean
        get() = selectedDirectParticipant != null && firstMessage.isNotBlank() && !isCreatingRoom

    val canSubmitGroup: Boolean
        get() = selectedGroupParticipants.isNotEmpty() && !isCreatingRoom

    val canSubmit: Boolean
        get() = when (mode) {
            NewChatMode.DIRECT -> canSubmitDirect
            NewChatMode.GROUP -> canSubmitGroup
        }
}
