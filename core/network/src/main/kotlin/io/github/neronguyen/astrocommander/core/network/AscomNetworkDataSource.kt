package io.github.neronguyen.astrocommander.core.network

import arrow.core.raise.Raise
import io.github.neronguyen.astrocommander.core.model.error.DataError
import io.github.neronguyen.astrocommander.core.network.model.PlaceholderJson

interface AscomNetworkDataSource {

    context(raise: Raise<DataError.Network>)
    suspend fun getPlaceholderJson(id: Int): PlaceholderJson

    context(raise: Raise<DataError.Network>)
    suspend fun getPlaceholderJsonList(): List<PlaceholderJson>
}
