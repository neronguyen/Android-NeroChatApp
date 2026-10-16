package io.github.neronguyen.chat.feature.chat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.neronguyen.chat.feature.chat.presentation.list.ChatListRoute
import io.github.neronguyen.chat.feature.chat.presentation.list.ChatListViewModel

fun EntryProviderScope<NavKey>.chatSection(
    onOpenConversation: (roomId: String) -> Unit,
    onNewChatClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    entry<ChatRoute.ChatList> {
        val viewModel: ChatListViewModel = hiltViewModel()
        ChatListRoute(
            viewModel = viewModel,
            onOpenConversation = onOpenConversation,
            onNewChatClick = onNewChatClick,
            onAccountClick = onAccountClick,
        )
    }
}

@Composable
fun ChatNavGraph(
    modifier: Modifier = Modifier,
    startRoute: ChatRoute = ChatRoute.ChatList,
    onOpenConversation: (roomId: String) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(startRoute)

    val entryProvider = remember(onOpenConversation, onNewChatClick, onAccountClick) {
        entryProvider {
            chatSection(
                onOpenConversation = onOpenConversation,
                onNewChatClick = onNewChatClick,
                onAccountClick = onAccountClick,
            )
        }
    }

    val saveableStateHolderDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<NavKey>()
    val decorators = remember(saveableStateHolderDecorator, viewModelStoreDecorator) {
        listOf(saveableStateHolderDecorator, viewModelStoreDecorator)
    }
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = decorators,
        entryProvider = entryProvider,
    )

    NavDisplay(
        entries = entries,
        onBack = { backStack.pop() },
        modifier = modifier,
    )
}

private fun NavBackStack<NavKey>.pop() {
    if (size > 1) {
        removeAt(lastIndex)
    }
}
