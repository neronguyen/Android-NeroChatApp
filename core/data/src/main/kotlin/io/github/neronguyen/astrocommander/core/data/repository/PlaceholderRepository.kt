package io.github.neronguyen.astrocommander.core.data.repository

import arrow.core.Either
import io.github.neronguyen.astrocommander.core.model.Placeholder
import io.github.neronguyen.astrocommander.core.model.error.DataError
import kotlinx.coroutines.flow.Flow

interface PlaceholderRepository {

    fun observePlaceholderList(): Flow<List<Placeholder>>

    suspend fun getPlaceholder(id: Long): Placeholder?

    suspend fun syncPlaceholderList(): Either<DataError, Unit>
}
