package io.github.neronguyen.astrocommander.core.data.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.neronguyen.astrocommander.core.database.dao.PlaceholderJsonDao
import io.github.neronguyen.astrocommander.core.database.model.PlaceholderJsonEntity
import io.github.neronguyen.astrocommander.core.model.Placeholder
import io.github.neronguyen.astrocommander.core.model.error.DataError
import io.github.neronguyen.astrocommander.core.network.AscomNetworkDataSource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstPlaceholderRepository @Inject constructor(
    private val networkDataSource: AscomNetworkDataSource,
    private val placeholderJsonDao: PlaceholderJsonDao,
) : PlaceholderRepository {

    override fun observePlaceholderList(): Flow<List<Placeholder>> {
        return placeholderJsonDao.observePlaceholderJsonList().map { entities ->
            entities.map { it.asExternalModel() }
        }
    }

    override suspend fun getPlaceholder(id: Long): Placeholder? {
        return placeholderJsonDao.getPlaceholderJson(id)?.asExternalModel()
    }

    override suspend fun syncPlaceholderList(): Either<DataError, Unit> = either {
        val networkList = networkDataSource.getPlaceholderJsonList()
        networkList.forEach { networkModel ->
            placeholderJsonDao.upsertPlaceholderJson(
                PlaceholderJsonEntity(
                    id = networkModel.id,
                    title = networkModel.title,
                    completed = networkModel.completed
                )
            )
        }
    }
}

fun PlaceholderJsonEntity.asExternalModel() = Placeholder(
    id = id,
    title = title,
    completed = completed
)
