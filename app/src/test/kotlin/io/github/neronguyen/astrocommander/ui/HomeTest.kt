package io.github.neronguyen.astrocommander.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.neronguyen.astrocommander.HomeScreen
import io.github.neronguyen.astrocommander.core.model.Placeholder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class HomeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_isDisplayed() {
        composeTestRule.setContent {
            HomeScreen(
                list = emptyList(),
                error = "Loading...",
                onRefresh = {},
                onItemClick = {}
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun listItems_areDisplayed() {
        val list = listOf(
            Placeholder(id = 1, title = "First Item", completed = false),
            Placeholder(id = 2, title = "Second Item", completed = true)
        )

        composeTestRule.setContent {
            HomeScreen(
                list = list,
                error = "",
                onRefresh = {},
                onItemClick = {}
            )
        }

        composeTestRule.onNodeWithText("ID: 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("First Item").assertIsDisplayed()
        composeTestRule.onNodeWithText("ID: 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Item").assertIsDisplayed()
    }

    @Test
    fun clickItem_triggersCallback() {
        val item = Placeholder(id = 1, title = "Click Me", completed = false)
        var clickedItem: Placeholder? = null

        composeTestRule.setContent {
            HomeScreen(
                list = listOf(item),
                error = "",
                onRefresh = {},
                onItemClick = { clickedItem = it }
            )
        }

        composeTestRule.onNodeWithText("Click Me").performClick()

        assertEquals(item, clickedItem)
    }

    @Test
    fun clickRefresh_triggersCallback() {
        var refreshCalled = false

        composeTestRule.setContent {
            HomeScreen(
                list = emptyList(),
                error = "",
                onRefresh = { refreshCalled = true },
                onItemClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Reload").performClick()

        assertEquals(true, refreshCalled)
    }
}
