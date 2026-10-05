package io.github.neronguyen.astrocommander.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.neronguyen.astrocommander.DetailRoute
import io.github.neronguyen.astrocommander.DetailViewModel
import io.github.neronguyen.astrocommander.HomeRoute
import io.github.neronguyen.astrocommander.MainViewModel
import io.github.neronguyen.astrocommander.Route
import io.github.neronguyen.astrocommander.core.model.Placeholder
import io.github.neronguyen.astrocommander.fakes.FakePlaceholderRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navFromHomeToDetailAndBack() {
        val repository = FakePlaceholderRepository()
        val mainViewModel = MainViewModel(repository)
        val detailViewModel = DetailViewModel(repository)

        repository.emit(listOf(Placeholder(id = 1, title = "Item 1", completed = false)))

        composeTestRule.setContent {
            val backStack = rememberNavBackStack(Route.Home)
            val decorators = listOf<NavEntryDecorator<NavKey>>(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )

            val provider = entryProvider<NavKey> {
                entry<Route.Home> {
                    HomeRoute(
                        viewModel = mainViewModel,
                        onItemClick = { item -> backStack.add(Route.Detail(item)) }
                    )
                }

                entry<Route.Detail> { key ->
                    DetailRoute(
                        viewModel = detailViewModel,
                        item = key.item,
                        onBack = { backStack.removeAt(backStack.size - 1) })
                }
            }

            val entries = rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = decorators,
                entryProvider = provider
            )

            NavDisplay(
                entries = entries,
                onBack = { backStack.removeLastOrNull() }
            )
        }

        // 1. Verify Home is shown
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID: 1").assertIsDisplayed()

        // 2. Click item to navigate to Detail
        composeTestRule.onNodeWithText("ID: 1").performClick()

        // 3. Verify Detail is shown
        composeTestRule.onNodeWithText("Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID: 1").assertIsDisplayed()

        // 4. Click Back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // 5. Verify Home is shown again
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }
}
