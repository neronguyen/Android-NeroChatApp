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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePlaceholderRepository
    private lateinit var viewModel: DetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePlaceholderRepository()
        viewModel = DetailViewModel(placeholderRepository = repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setItem should update item with prefix if it exists in repository`() = runTest {
        viewModel.item.test {
            assertNull(awaitItem())

            val item = Placeholder(id = 1L, title = "Existing", completed = false)
            repository.emit(listOf(item))

            viewModel.setItem(item)
            testScheduler.runCurrent()

            assertEquals("Details-Existing", awaitItem()?.title)
        }
    }

    @Test
    fun `setItem should update item without prefix if it does not exist in repository`() = runTest {
        viewModel.item.test {
            assertNull(awaitItem())

            val item = Placeholder(id = 1L, title = "New", completed = false)
            // Repository is empty

            viewModel.setItem(item)
            testScheduler.runCurrent()

            assertEquals("New", awaitItem()?.title)
        }
    }
}
