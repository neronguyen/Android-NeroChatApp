package io.github.neronguyen.astrocommander.fakes

import arrow.core.Either
import arrow.core.right
import io.github.neronguyen.astrocommander.core.data.repository.PlaceholderRepository
import io.github.neronguyen.astrocommander.core.model.Placeholder
import io.github.neronguyen.astrocommander.core.model.error.DataError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

class FakePlaceholderRepository : PlaceholderRepository {

    private val _placeholders = MutableStateFlow<Map<Long, Placeholder>>(emptyMap())

    var syncDelay = 0L

    override fun observePlaceholderList(): Flow<List<Placeholder>> {
        return _placeholders.map { it.values.toList() }
    }

    override suspend fun getPlaceholder(id: Long): Placeholder? {
        return _placeholders.value[id]
    }

    override suspend fun syncPlaceholderList(): Either<DataError, Unit> {
        if (syncDelay > 0) delay(syncDelay.milliseconds)
        return Unit.right()
    }

    fun emit(placeholders: List<Placeholder>) {
        _placeholders.update {
            placeholders.associateBy { it.id }
        }
    }
}
