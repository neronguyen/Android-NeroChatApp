package io.github.neronguyen.chat.core.datastore

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.neronguyen.chat.core.datastore.model.UserData
import io.github.neronguyen.chat.core.datastore.model.toDomain
import io.github.neronguyen.chat.core.model.AuthState
import io.github.neronguyen.chat.core.model.User
import io.github.neronguyen.chat.core.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

@Singleton
internal class PreferencesTokenDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) : TokenDataSource {

    private val dataStore = context.authDataStore
    private val json = Json { ignoreUnknownKeys = true }

    override val authState: Flow<AuthState> = dataStore.data.map { preferences ->
        val encryptedAccessToken =
            preferences[KEY_ENCRYPTED_ACCESS_TOKEN] ?: return@map AuthState.Unauthenticated
        val userDataJson = preferences[KEY_USER_DATA] ?: return@map AuthState.Unauthenticated

        val accessToken = decryptString(encryptedAccessToken)
        if (accessToken.isBlank()) return@map AuthState.Unauthenticated

        try {
            val userData = json.decodeFromString<UserData>(userDataJson)
            AuthState.Authenticated(
                user = userData.toDomain(),
                accessToken = accessToken
            )
        } catch (_: Exception) {
            AuthState.Unauthenticated
        }
    }

    override suspend fun getAccessToken(): String? {
        val preferences = dataStore.data.firstOrNull() ?: return null
        val encrypted = preferences[KEY_ENCRYPTED_ACCESS_TOKEN] ?: return null
        return decryptString(encrypted).ifBlank { null }
    }

    override suspend fun getRefreshToken(): String? {
        val preferences = dataStore.data.firstOrNull() ?: return null
        val encrypted = preferences[KEY_ENCRYPTED_REFRESH_TOKEN] ?: return null
        return decryptString(encrypted).ifBlank { null }
    }

    override suspend fun saveAuthData(user: User, accessToken: String, refreshToken: String) {
        val encryptedAccessToken = encryptString(accessToken)
        val encryptedRefreshToken = encryptString(refreshToken)

        val userData = UserData(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            isEmailVerified = user.isEmailVerified
        )
        val userDataJson = json.encodeToString(UserData.serializer(), userData)

        dataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_ACCESS_TOKEN] = encryptedAccessToken
            preferences[KEY_ENCRYPTED_REFRESH_TOKEN] = encryptedRefreshToken
            preferences[KEY_USER_DATA] = userDataJson
        }
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        val encryptedAccessToken = encryptString(accessToken)
        val encryptedRefreshToken = encryptString(refreshToken)

        dataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_ACCESS_TOKEN] = encryptedAccessToken
            preferences[KEY_ENCRYPTED_REFRESH_TOKEN] = encryptedRefreshToken
        }
    }

    override suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun encryptString(rawText: String): String {
        if (rawText.isEmpty()) return ""
        val encryptedBytes = cryptoManager.encrypt(rawText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    private fun decryptString(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
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
