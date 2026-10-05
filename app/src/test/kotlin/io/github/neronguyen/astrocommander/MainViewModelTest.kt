package io.github.neronguyen.astrocommander

import app.cash.turbine.test
import io.github.neronguyen.astrocommander.core.model.Placeholder
import io.github.neronguyen.astrocommander.fakes.FakePlaceholderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePlaceholderRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePlaceholderRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows empty list and loading message`() = runTest {
        repository.syncDelay = 100
        viewModel = MainViewModel(placeholderRepository = repository)

        // Use backgroundScope to collect flows that never complete (like StateFlow)
        // Or just use Turbine test which handles it.

        viewModel.list.test {
            assertEquals(emptyList(), awaitItem())
        }

        viewModel.error.test {
            assertEquals("", awaitItem()) // Initial value

            // Advance time to allow refresh() to run
            testScheduler.runCurrent()
            assertEquals("Loading...", awaitItem())

            testScheduler.advanceTimeBy(101)
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun `list state updates when repository emits data`() = runTest {
        viewModel = MainViewModel(placeholderRepository = repository)
        testScheduler.runCurrent() // let init sync finish

        viewModel.list.test {
            assertEquals(emptyList(), awaitItem())

            val placeholders = listOf(
                Placeholder(id = 1L, title = "Test", completed = false)
            )
            repository.emit(placeholders)

            assertEquals(placeholders, awaitItem())
        }
    }

    @Test
    fun `refresh updates error state correctly on success`() = runTest {
        viewModel = MainViewModel(placeholderRepository = repository)
        testScheduler.runCurrent() // let init sync finish

        viewModel.error.test {
            assertEquals("", awaitItem()) // current value

            repository.syncDelay = 100
            viewModel.refresh()

            testScheduler.runCurrent()
            assertEquals("Loading...", awaitItem())

            testScheduler.advanceTimeBy(101)
            assertEquals("", awaitItem())
        }
    }
}
