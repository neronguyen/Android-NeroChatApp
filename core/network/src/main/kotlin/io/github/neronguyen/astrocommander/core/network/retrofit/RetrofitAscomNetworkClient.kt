package io.github.neronguyen.astrocommander.core.network.retrofit

import arrow.core.raise.Raise
import io.github.neronguyen.astrocommander.core.model.error.DataError
import io.github.neronguyen.astrocommander.core.network.AscomNetworkDataSource
import io.github.neronguyen.astrocommander.core.network.model.PlaceholderJson
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private interface RetrofitAscomNetworkApi {

    @GET("todos/{id}")
    suspend fun getPlaceholderJson(
        @Path("id") id: Int
    ): Response<PlaceholderJson>

    @GET("todos")
    suspend fun getPlaceholderJsonList(): Response<List<PlaceholderJson>>
}

internal class RetrofitAscomNetworkClient(
    okHttpClient: OkHttpClient,
    networkJson: Json
) : AscomNetworkDataSource {

    private val networkClient = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .client(okHttpClient)
        .addConverterFactory(
            networkJson.asConverterFactory("application/json".toMediaType()),
        )
        .build()
        .create(RetrofitAscomNetworkApi::class.java)


    context(raise: Raise<DataError.Network>)
    override suspend fun getPlaceholderJson(id: Int): PlaceholderJson {
        return safeCall {
            networkClient.getPlaceholderJson(id)
        }
    }

    context(raise: Raise<DataError.Network>)
    override suspend fun getPlaceholderJsonList(): List<PlaceholderJson> {
        return safeCall {
            networkClient.getPlaceholderJsonList()
        }
    }

    context(raise: Raise<DataError.Network>)
    private suspend inline fun <reified T> safeCall(
        execute: suspend () -> Response<T>
    ): T {
        try {
            val response = execute()
            if (!response.isSuccessful) {
                raise.raise(DataError.Network.Unknown)
            }

            return response.body()
                ?: raise.raise(DataError.Network.Unknown)

        } catch (_: SerializationException) {
            raise.raise(DataError.Network.Serialization)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            raise.raise(e.asNetworkError())
        }
    }

    private fun Exception.asNetworkError(): DataError.Network = when (this) {
        is UnknownHostException,
        is UnresolvedAddressException,
        is ConnectException -> DataError.Network.NoInternet

        is SocketTimeoutException -> DataError.Network.RequestTimeout

        else -> DataError.Network.Unknown
    }
}
