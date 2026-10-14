package io.github.neronguyen.chat.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.neronguyen.chat.core.common.network.ChatDispatchers
import io.github.neronguyen.chat.core.common.network.Dispatcher
import io.github.neronguyen.chat.core.datastore.model.UserData
import io.github.neronguyen.chat.core.datastore.model.toDomain
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.security.CryptoManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

@Singleton
internal class PreferencesTokenDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    @Dispatcher(ChatDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : TokenDataSource {

    private val dataStore = context.authDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val cachedAccessToken = AtomicReference<String?>(null)

    override val currentAccessToken: String?
        get() = cachedAccessToken.get()

    override val authState: Flow<AuthState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val encryptedAccessToken =
                preferences[KEY_ENCRYPTED_ACCESS_TOKEN] ?: return@map AuthState.Unauthenticated
            val userDataJson = preferences[KEY_USER_DATA] ?: return@map AuthState.Unauthenticated

            val accessToken = decryptString(encryptedAccessToken)
            if (accessToken.isBlank()) return@map AuthState.Unauthenticated

            try {
                val userData = json.decodeFromString<UserData>(userDataJson)
                AuthState.Authenticated(
                    user = userData.toDomain(),
                    accessToken = accessToken,
                )
            } catch (_: Exception) {
                AuthState.Unauthenticated
            }
        }
        .onEach { state ->
            if (state is AuthState.Authenticated) {
                cachedAccessToken.set(state.accessToken)
            } else {
                cachedAccessToken.set(null)
            }
        }
        .flowOn(ioDispatcher)

    override suspend fun getAccessToken(): String? = withContext(ioDispatcher) {
        val cached = cachedAccessToken.get()
        if (!cached.isNullOrBlank()) return@withContext cached

        val preferences = dataStore.data.firstOrNull() ?: return@withContext null
        val encrypted = preferences[KEY_ENCRYPTED_ACCESS_TOKEN] ?: return@withContext null
        val decrypted = decryptString(encrypted).ifBlank { null }
        cachedAccessToken.set(decrypted)
        decrypted
    }

    override suspend fun getRefreshToken(): String? = withContext(ioDispatcher) {
        val preferences = dataStore.data.firstOrNull() ?: return@withContext null
        val encrypted = preferences[KEY_ENCRYPTED_REFRESH_TOKEN] ?: return@withContext null
        decryptString(encrypted).ifBlank { null }
    }

    override suspend fun saveAuthData(user: User, accessToken: String, refreshToken: String) =
        withContext(ioDispatcher) {
            cachedAccessToken.set(accessToken)
            val encryptedAccessToken = encryptString(accessToken)
            val encryptedRefreshToken = encryptString(refreshToken)

            val userData = UserData(
                userId = user.id,
                email = user.email,
                displayName = user.displayName,
                isEmailVerified = user.isEmailVerified,
            )
            val userDataJson = json.encodeToString(UserData.serializer(), userData)

            dataStore.edit { preferences ->
                preferences[KEY_ENCRYPTED_ACCESS_TOKEN] = encryptedAccessToken
                preferences[KEY_ENCRYPTED_REFRESH_TOKEN] = encryptedRefreshToken
                preferences[KEY_USER_DATA] = userDataJson
            }
            Unit
        }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) =
        withContext(ioDispatcher) {
            cachedAccessToken.set(accessToken)
            val encryptedAccessToken = encryptString(accessToken)
            val encryptedRefreshToken = encryptString(refreshToken)

            dataStore.edit { preferences ->
                preferences[KEY_ENCRYPTED_ACCESS_TOKEN] = encryptedAccessToken
                preferences[KEY_ENCRYPTED_REFRESH_TOKEN] = encryptedRefreshToken
            }
            Unit
        }

    override suspend fun clearAuthData() {
        cachedAccessToken.set(null)
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun encryptString(rawText: String): String {
        if (rawText.isEmpty()) return ""

        val encryptedBytes = cryptoManager.encrypt(rawText.encodeToByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    private fun decryptString(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""

        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            decryptedBytes.decodeToString()
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private val KEY_ENCRYPTED_ACCESS_TOKEN = stringPreferencesKey("encrypted_access_token")
        private val KEY_ENCRYPTED_REFRESH_TOKEN = stringPreferencesKey("encrypted_refresh_token")
        private val KEY_USER_DATA = stringPreferencesKey("user_data")
    }
}
