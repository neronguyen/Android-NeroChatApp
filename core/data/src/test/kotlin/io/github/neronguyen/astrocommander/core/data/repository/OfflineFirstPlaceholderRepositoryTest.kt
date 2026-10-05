package io.github.neronguyen.astrocommander.core.data.repository

import io.github.neronguyen.astrocommander.core.data.repository.fakes.FakeAscomNetworkDataSource
import io.github.neronguyen.astrocommander.core.data.repository.fakes.FakePlaceholderJsonDao
import io.github.neronguyen.astrocommander.core.network.model.PlaceholderJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class OfflineFirstPlaceholderRepositoryTest {

    private lateinit var networkDataSource: FakeAscomNetworkDataSource
    private lateinit var placeholderJsonDao: FakePlaceholderJsonDao
    private lateinit var repository: OfflineFirstPlaceholderRepository

    @Before
    fun setUp() {
        networkDataSource = FakeAscomNetworkDataSource()
        placeholderJsonDao = FakePlaceholderJsonDao()
        repository = OfflineFirstPlaceholderRepository(
            networkDataSource = networkDataSource,
            placeholderJsonDao = placeholderJsonDao
        )
    }

    @Test
    fun syncPlaceholderList_upsertsDataFromNetworkToDao() = runTest {
        val networkData = listOf(
            PlaceholderJson(id = 1L, title = "Title 1", completed = false),
            PlaceholderJson(id = 2L, title = "Title 2", completed = true)
        )
        networkDataSource.placeholders.addAll(networkData)

        repository.syncPlaceholderList()

        val savedData = placeholderJsonDao.observePlaceholderJsonList().first()
        assertEquals(2, savedData.size)
        assertEquals("Title 1", savedData[0].title)
        assertEquals("Title 2", savedData[1].title)
    }

    @Test
    fun observePlaceholderList_mapsEntitiesToExternalModels() = runTest {
        repository.syncPlaceholderList() // Sync empty first

        val networkData = listOf(
            PlaceholderJson(id = 1L, title = "Title 1", completed = false)
        )
        networkDataSource.placeholders.addAll(networkData)
        repository.syncPlaceholderList()

        val observedData = repository.observePlaceholderList().first()
        assertEquals(1, observedData.size)
        assertEquals(1L, observedData[0].id)
        assertEquals("Title 1", observedData[0].title)
        assertEquals(false, observedData[0].completed)
    }
}
