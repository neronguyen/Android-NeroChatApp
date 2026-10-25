package io.github.neronguyen.chat.feature.chat.presentation.newchat

import arrow.core.Either
import io.github.neronguyen.chat.core.data.repository.AuthRepository
import io.github.neronguyen.chat.core.data.repository.ChatRoomRepository
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.ChatParticipant
import io.github.neronguyen.chat.core.model.ChatRoom
import io.github.neronguyen.chat.core.model.ChatRoomType
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.model.error.AuthError
import io.github.neronguyen.chat.core.model.error.ChatError
import io.github.neronguyen.chat.core.model.error.DataError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NewChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeChatRoomRepository: FakeNewChatRoomRepository
    private lateinit var fakeAuthRepository: FakeNewChatAuthRepository
    private lateinit var viewModel: NewChatViewModel

    private val currentUser = User(
        id = "user-me",
        email = "me@example.com",
        displayName = "Me",
        isEmailVerified = true,
    )
    private val meParticipant = ChatParticipant(
        userId = currentUser.id,
        email = currentUser.email,
        displayName = currentUser.displayName,
    )
    private val elenaParticipant = ChatParticipant(
        userId = "user-elena",
        email = "elena@example.com",
        displayName = "Elena Rostova",
    )
    private val marcusParticipant = ChatParticipant(
        userId = "user-marcus",
        email = "marcus@example.com",
        displayName = "Marcus Vance",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeChatRoomRepository = FakeNewChatRoomRepository()
        fakeAuthRepository = FakeNewChatAuthRepository(
            initialState = AuthState.Authenticated(currentUser, "token"),
        )
        viewModel = NewChatViewModel(
            chatRoomRepository = fakeChatRoomRepository,
            authRepository = fakeAuthRepository,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search with empty or invalid email sets validationError without calling repository`() =
        runTest {
            testScheduler.advanceUntilIdle()

            viewModel.onEvent(NewChatUiEvent.SearchByEmail)
            assertEquals(
                NewChatValidationError.EmptySearchEmail,
                viewModel.uiState.value.validationError,
            )
            assertEquals(0, fakeChatRoomRepository.searchEmails.size)

            viewModel.onEmailSearchQueryChanged("invalid-email")
            viewModel.onEvent(NewChatUiEvent.SearchByEmail)
            assertEquals(
                NewChatValidationError.InvalidSearchEmail,
                viewModel.uiState.value.validationError,
            )
            assertEquals(0, fakeChatRoomRepository.searchEmails.size)
        }

    @Test
    fun `search with own email sets SelfParticipantNotAllowed`() = runTest {
        testScheduler.advanceUntilIdle()

        viewModel.onEmailSearchQueryChanged("ME@example.com")
        viewModel.onEvent(NewChatUiEvent.SearchByEmail)

        assertEquals(
            NewChatValidationError.SelfParticipantNotAllowed,
            viewModel.uiState.value.validationError,
        )
        assertNull(viewModel.uiState.value.searchedParticipant)
        assertEquals(0, fakeChatRoomRepository.searchEmails.size)
    }

    @Test
    fun `direct mode search auto-selects recipient, supports toggle unselect-select, and creates direct room`() =
        runTest {
            testScheduler.advanceUntilIdle()
            fakeChatRoomRepository.participantsByEmail["elena@example.com"] =
                Either.Right(elenaParticipant)

            viewModel.onEmailSearchQueryChanged("elena@example.com")
            viewModel.onEvent(NewChatUiEvent.SearchByEmail)
            testScheduler.advanceUntilIdle()

            val stateAfterSearch = viewModel.uiState.value
            assertEquals("user-elena", stateAfterSearch.searchedParticipant?.userId)
            assertEquals("user-elena", stateAfterSearch.selectedDirectParticipant?.userId)
            assertTrue(stateAfterSearch.isSearchedParticipantSelected)
            assertFalse(stateAfterSearch.canSubmitDirect)

            // Toggle to unselect
            viewModel.onEvent(NewChatUiEvent.ToggleSearchedParticipant)
            assertNull(viewModel.uiState.value.selectedDirectParticipant)
            assertFalse(viewModel.uiState.value.isSearchedParticipantSelected)

            // Toggle to re-select
            viewModel.onEvent(NewChatUiEvent.ToggleSearchedParticipant)
            assertEquals("user-elena", viewModel.uiState.value.selectedDirectParticipant?.userId)
            assertTrue(viewModel.uiState.value.isSearchedParticipantSelected)

            // Enter first message and submit
            viewModel.onFirstMessageChanged("  Hello Elena!  ")
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canSubmitDirect)

            viewModel.onEvent(NewChatUiEvent.SubmitChat)
            testScheduler.advanceUntilIdle()

            assertEquals("user-elena", fakeChatRoomRepository.lastDirectTargetUserId)
            assertEquals("Hello Elena!", fakeChatRoomRepository.lastDirectMessage)
            assertEquals("room-direct-1", viewModel.uiState.value.createdRoomId)

            viewModel.onChatCreatedHandled()
            assertNull(viewModel.uiState.value.createdRoomId)
        }

    @Test
    fun `group mode supports selecting and unselecting searched participant and removing selected participant chip`() =
        runTest {
            testScheduler.advanceUntilIdle()
            fakeChatRoomRepository.participantsByEmail["elena@example.com"] =
                Either.Right(elenaParticipant)
            fakeChatRoomRepository.participantsByEmail["marcus@example.com"] =
                Either.Right(marcusParticipant)

            viewModel.onEvent(NewChatUiEvent.SelectMode(NewChatMode.GROUP))
            assertEquals(NewChatMode.GROUP, viewModel.uiState.value.mode)

            // Search Elena in group mode -> found but not yet selected
            viewModel.onEmailSearchQueryChanged("elena@example.com")
            viewModel.onEvent(NewChatUiEvent.SearchByEmail)
            testScheduler.advanceUntilIdle()

            assertEquals("user-elena", viewModel.uiState.value.searchedParticipant?.userId)
            assertFalse(viewModel.uiState.value.isSearchedParticipantSelected)
            assertTrue(viewModel.uiState.value.selectedGroupParticipants.isEmpty())

            // Toggle to select Elena
            viewModel.onEvent(NewChatUiEvent.ToggleSearchedParticipant)
            assertTrue(viewModel.uiState.value.isSearchedParticipantSelected)
            assertEquals(
                listOf("user-elena"),
                viewModel.uiState.value.selectedGroupParticipants.map { it.userId },
            )

            // Toggle again to unselect Elena
            viewModel.onEvent(NewChatUiEvent.ToggleSearchedParticipant)
            assertFalse(viewModel.uiState.value.isSearchedParticipantSelected)
            assertTrue(viewModel.uiState.value.selectedGroupParticipants.isEmpty())

            // Select Elena again, then search and select Marcus
            viewModel.onEvent(NewChatUiEvent.ToggleSearchedParticipant)
            viewModel.onEmailSearchQueryChanged("marcus@example.com")
            viewModel.onEvent(NewChatUiEvent.SearchByEmail)
            testScheduler.advanceUntilIdle()

            assertEquals("user-marcus", viewModel.uiState.value.searchedParticipant?.userId)
            assertFalse(viewModel.uiState.value.isSearchedParticipantSelected)

            viewModel.onEvent(NewChatUiEvent.ToggleSearchedParticipant)
            assertTrue(viewModel.uiState.value.isSearchedParticipantSelected)
            assertEquals(
                listOf("user-elena", "user-marcus"),
                viewModel.uiState.value.selectedGroupParticipants.map { it.userId },
            )

            // Remove Marcus via chip removal -> also updates isSearchedParticipantSelected to false
            viewModel.onEvent(NewChatUiEvent.RemoveGroupParticipant("user-marcus"))
            assertFalse(viewModel.uiState.value.isSearchedParticipantSelected)
            assertEquals(
                listOf("user-elena"),
                viewModel.uiState.value.selectedGroupParticipants.map { it.userId },
            )

            // Dismiss search result card -> keeps selectedGroupParticipants intact
            viewModel.onEvent(NewChatUiEvent.ClearSearchResult)
            assertNull(viewModel.uiState.value.searchedParticipant)
            assertEquals(
                listOf("user-elena"),
                viewModel.uiState.value.selectedGroupParticipants.map { it.userId },
            )

            // Set group name and create group
            viewModel.onGroupNameChanged("  Core Team  ")
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canSubmitGroup)

            viewModel.onEvent(NewChatUiEvent.SubmitChat)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("user-elena"), fakeChatRoomRepository.lastGroupParticipantIds)
            assertEquals("Core Team", fakeChatRoomRepository.lastGroupName)
            assertEquals("room-group-1", viewModel.uiState.value.createdRoomId)
        }

    @Test
    fun `search network error and create room network error are exposed in uiState`() = runTest {
        testScheduler.advanceUntilIdle()
        fakeChatRoomRepository.participantsByEmail["missing@example.com"] =
            Either.Left(ChatError.ParticipantNotFound)

        viewModel.onEmailSearchQueryChanged("missing@example.com")
        viewModel.onEvent(NewChatUiEvent.SearchByEmail)
        testScheduler.advanceUntilIdle()

        assertEquals(ChatError.ParticipantNotFound, viewModel.uiState.value.searchError)
        assertNull(viewModel.uiState.value.searchedParticipant)

        // Now succeed search, but fail direct room creation
        fakeChatRoomRepository.participantsByEmail["elena@example.com"] =
            Either.Right(elenaParticipant)
        fakeChatRoomRepository.createDirectResult =
            Either.Left(ChatError.Data(DataError.Connectivity.NoInternet))

        viewModel.onEmailSearchQueryChanged("elena@example.com")
        viewModel.onEvent(NewChatUiEvent.SearchByEmail)
        testScheduler.advanceUntilIdle()

        viewModel.onFirstMessageChanged("Hello")
        testScheduler.advanceUntilIdle()

        viewModel.onEvent(NewChatUiEvent.SubmitChat)
        testScheduler.advanceUntilIdle()

        assertEquals(ChatError.Data(DataError.Connectivity.NoInternet), viewModel.uiState.value.createError)
        assertNull(viewModel.uiState.value.createdRoomId)
    }

    private inner class FakeNewChatRoomRepository : ChatRoomRepository {
        val searchEmails = mutableListOf<String>()
        val participantsByEmail =
            mutableMapOf<String, Either<ChatError.FindParticipant, ChatParticipant>>()

        var lastDirectTargetUserId: String? = null
        var lastDirectMessage: String? = null
        var createDirectResult: Either<ChatError.CreateRoom, ChatRoom> = Either.Right(
            ChatRoom(
                id = "room-direct-1",
                type = ChatRoomType.DIRECT,
                creator = meParticipant,
                participants = listOf(meParticipant, elenaParticipant),
            )
        )

        var lastGroupParticipantIds: List<String>? = null
        var lastGroupName: String? = null
        var createGroupResult: Either<ChatError.CreateRoom, ChatRoom> = Either.Right(
            ChatRoom(
                id = "room-group-1",
                type = ChatRoomType.GROUP,
                name = "Core Team",
                creator = meParticipant,
                participants = listOf(meParticipant, elenaParticipant),
            )
        )

        override fun getChats(): Flow<List<ChatRoom>> = MutableStateFlow(emptyList())

        override suspend fun syncChats(): Either<DataError, Unit> = Either.Right(Unit)

        override suspend fun findChatParticipantByEmail(
            email: String,
        ): Either<ChatError.FindParticipant, ChatParticipant> {
            searchEmails.add(email)
            return participantsByEmail[email] ?: Either.Left(ChatError.ParticipantNotFound)
        }

        override suspend fun createDirectChatRoom(
            targetUserId: String,
            message: String,
        ): Either<ChatError.CreateRoom, ChatRoom> {
            lastDirectTargetUserId = targetUserId
            lastDirectMessage = message
            return createDirectResult
        }

        override suspend fun createGroupChatRoom(
            participantIds: List<String>,
            name: String?,
        ): Either<ChatError.CreateRoom, ChatRoom> {
            lastGroupParticipantIds = participantIds
            lastGroupName = name
            return createGroupResult
        }

        override fun getChatRoom(roomId: String): Flow<ChatRoom?> =
            kotlinx.coroutines.flow.emptyFlow()

        override suspend fun syncChatRoom(roomId: String): Either<ChatError.SyncRoom, Unit> =
            Either.Right(Unit)

        override suspend fun addParticipantsToGroup(
            roomId: String,
            userIds: List<String>
        ): Either<ChatError.AddParticipants, ChatRoom> = throw UnsupportedOperationException()

        override suspend fun leaveChatRoom(roomId: String): Either<ChatError.LeaveRoom, Unit> =
            Either.Right(Unit)
    }


    private class FakeNewChatAuthRepository(
        initialState: AuthState,
    ) : AuthRepository {
        private val stateFlow = MutableStateFlow(initialState)

        override fun getAuthState(): Flow<AuthState> = stateFlow

        override suspend fun login(
            email: String,
            password: String,
        ): Either<AuthError.Login, User> {
            throw UnsupportedOperationException()
        }

        override suspend fun register(
            email: String,
            displayName: String,
            password: String,
        ): Either<AuthError.Register, User> {
            throw UnsupportedOperationException()
        }

        override suspend fun logout(): Either<DataError, Unit> = Either.Right(Unit)
    }
}
