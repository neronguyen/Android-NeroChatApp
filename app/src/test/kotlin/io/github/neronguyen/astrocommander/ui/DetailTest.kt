package io.github.neronguyen.astrocommander.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.neronguyen.astrocommander.DetailScreen
import io.github.neronguyen.astrocommander.core.model.Placeholder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DetailTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun itemDetails_areDisplayed() {
        val item = Placeholder(id = 42, title = "The Meaning of Life", completed = true)

        composeTestRule.setContent {
            DetailScreen(
                item = item,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("ID: 42").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title: The Meaning of Life").assertIsDisplayed()
        composeTestRule.onNodeWithText("Completed: true").assertIsDisplayed()
    }

    @Test
    fun clickBack_triggersCallback() {
        var backCalled = false
        val item = Placeholder(id = 1, title = "Back Test", completed = false)

        composeTestRule.setContent {
            DetailScreen(
                item = item,
                onBack = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(true, backCalled)
    }
}
