package io.github.neronguyen.astrocommander.core.data.repository.fakes

import arrow.core.raise.Raise
import io.github.neronguyen.astrocommander.core.model.error.DataError
import io.github.neronguyen.astrocommander.core.network.AscomNetworkDataSource
import io.github.neronguyen.astrocommander.core.network.model.PlaceholderJson

class FakeAscomNetworkDataSource : AscomNetworkDataSource {

    var placeholders = mutableListOf<PlaceholderJson>()

    context(raise: Raise<DataError.Network>)
    override suspend fun getPlaceholderJson(id: Int): PlaceholderJson {
        return placeholders.find { it.id == id.toLong() } ?: raise.raise(DataError.Network.Unknown)
    }

    context(raise: Raise<DataError.Network>)
    override suspend fun getPlaceholderJsonList(): List<PlaceholderJson> {
        return placeholders
    }
}
