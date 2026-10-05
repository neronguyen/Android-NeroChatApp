package io.github.neronguyen.chat.feature.auth.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.neronguyen.chat.feature.auth.presentation.login.LoginRoute
import io.github.neronguyen.chat.feature.auth.presentation.login.LoginViewModel
import io.github.neronguyen.chat.feature.auth.presentation.register.RegisterRoute
import io.github.neronguyen.chat.feature.auth.presentation.register.RegisterViewModel

fun EntryProviderScope<NavKey>.authSection(
    onAuthSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    entry<AuthRoute.Login> {
        val viewModel: LoginViewModel = hiltViewModel()
        LoginRoute(
            viewModel = viewModel,
            onNavigateToRegister = onNavigateToRegister,
            onLoginSuccess = onAuthSuccess
        )
    }
    entry<AuthRoute.Register> {
        val viewModel: RegisterViewModel = hiltViewModel()
        RegisterRoute(
            viewModel = viewModel,
            onNavigateToLogin = onNavigateToLogin,
            onRegisterSuccess = onNavigateToLogin
        )
    }
}

@Composable
fun AuthNavGraph(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    startRoute: AuthRoute = AuthRoute.Login
) {
    val backStack = rememberNavBackStack(startRoute)

    val entryProvider = remember(onAuthSuccess) {
        entryProvider {
            authSection(
                onAuthSuccess = onAuthSuccess,
                onNavigateToRegister = {
                    backStack.add(AuthRoute.Register)
                },
                onNavigateToLogin = {
                    backStack.popOrNavigateTo(AuthRoute.Login)
                }
            )
        }
    }

    val decorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>())
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = decorators,
        entryProvider = entryProvider
    )

    NavDisplay(
        entries = entries,
        onBack = { backStack.pop() },
        modifier = modifier
    )
}

private fun NavBackStack<NavKey>.pop() {
    if (size > 1) {
        removeAt(lastIndex)
    }
}

private fun NavBackStack<NavKey>.popOrNavigateTo(defaultRoute: NavKey) {
    if (size > 1) {
        removeAt(lastIndex)
    } else {
        add(defaultRoute)
    }
}
