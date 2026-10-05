package io.github.neronguyen.astrocommander.core.data.repository.fakes

import io.github.neronguyen.astrocommander.core.database.dao.PlaceholderJsonDao
import io.github.neronguyen.astrocommander.core.database.model.PlaceholderJsonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePlaceholderJsonDao : PlaceholderJsonDao {

    private val _data = MutableStateFlow<Map<Long, PlaceholderJsonEntity>>(emptyMap())

    override suspend fun upsertPlaceholderJson(placeholderJson: PlaceholderJsonEntity) {
        _data.update { it + (placeholderJson.id to placeholderJson) }
    }

    override fun observePlaceholderJsonList(): Flow<List<PlaceholderJsonEntity>> {
        return _data.map { it.values.toList() }
    }

    override suspend fun getPlaceholderJson(id: Long): PlaceholderJsonEntity? {
        return _data.value[id]
    }
}
