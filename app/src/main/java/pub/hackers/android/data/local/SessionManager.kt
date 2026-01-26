package pub.hackers.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val SESSION_TOKEN_KEY = stringPreferencesKey("session_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_HANDLE_KEY = stringPreferencesKey("user_handle")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
    }

    val sessionToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SESSION_TOKEN_KEY]
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SESSION_TOKEN_KEY] != null
    }

    val username: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    val userId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val userHandle: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_HANDLE_KEY]
    }

    val userName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userAvatar: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_AVATAR_KEY]
    }

    suspend fun saveSession(
        token: String,
        userId: String,
        username: String,
        handle: String,
        name: String,
        avatarUrl: String
    ) {
        dataStore.edit { preferences ->
            preferences[SESSION_TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
            preferences[USER_HANDLE_KEY] = handle
            preferences[USER_NAME_KEY] = name
            preferences[USER_AVATAR_KEY] = avatarUrl
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(SESSION_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(USER_HANDLE_KEY)
            preferences.remove(USER_NAME_KEY)
            preferences.remove(USER_AVATAR_KEY)
        }
    }
}
