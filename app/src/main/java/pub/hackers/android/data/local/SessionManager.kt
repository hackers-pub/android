package pub.hackers.android.data.local

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A signed-in account kept on the device. Several can be stored at once; exactly
 * one of them is active and its credentials are what the API client sends.
 */
@Immutable
data class StoredAccount(
    val token: String,
    val userId: String,
    val username: String,
    val handle: String,
    val name: String,
    val avatarUrl: String,
)

/**
 * Stores every signed-in account and tracks which one is active.
 *
 * The single-account keys (`session_token`, `user_id`, …) always mirror the
 * active account, so readers such as the auth interceptor, the notification
 * worker and FCM registration keep working without knowing about multiple
 * accounts. The full list lives under `accounts` as a JSON array. Installs that
 * predate multi-account support have only the mirror keys; [accounts] folds that
 * session into the list on read, and the next write persists it.
 */
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
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts")
        private const val TOKEN_PROPAGATION_TIMEOUT_MS = 2_000L
    }

    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val sessionToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[SESSION_TOKEN_KEY]
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SESSION_TOKEN_KEY] != null
    }

    val sessionTokenState: StateFlow<String?> =
        sessionToken.stateIn(appScope, SharingStarted.Eagerly, null)

    val isLoggedInState: StateFlow<Boolean> =
        isLoggedIn.stateIn(appScope, SharingStarted.Eagerly, false)

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

    /** Every stored account, in the order they were added. */
    val accounts: Flow<List<StoredAccount>> = dataStore.data.map { preferences ->
        readAccounts(preferences)
    }

    /**
     * Stores a new session and makes it the active account. Signing in again to
     * an account that is already stored replaces its token in place.
     */
    suspend fun saveSession(
        token: String,
        userId: String,
        username: String,
        handle: String,
        name: String,
        avatarUrl: String
    ) {
        val account = StoredAccount(
            token = token,
            userId = userId,
            username = username,
            handle = handle,
            name = name,
            avatarUrl = avatarUrl,
        )
        dataStore.edit { preferences ->
            val accounts = readAccounts(preferences)
            val index = accounts.indexOfFirst { it.userId == userId }
            val updated = if (index >= 0) {
                accounts.toMutableList().also { it[index] = account }
            } else {
                accounts + account
            }
            writeAccounts(preferences, updated)
            writeActive(preferences, account)
        }
        awaitActiveToken(token)
    }

    /**
     * Makes the stored account with [userId] active. Returns `false` if no such
     * account is stored.
     */
    suspend fun switchAccount(userId: String): Boolean {
        var target: StoredAccount? = null
        dataStore.edit { preferences ->
            val account = readAccounts(preferences).firstOrNull { it.userId == userId }
                ?: return@edit
            writeActive(preferences, account)
            target = account
        }
        val account = target ?: return false
        awaitActiveToken(account.token)
        return true
    }

    /**
     * Removes the active account. If other accounts are stored, the first one
     * becomes active and is returned; otherwise the device ends up signed out
     * and this returns `null`.
     */
    suspend fun clearSession(): StoredAccount? {
        var next: StoredAccount? = null
        dataStore.edit { preferences ->
            val activeUserId = preferences[USER_ID_KEY]
            val remaining = readAccounts(preferences).filter { it.userId != activeUserId }
            writeAccounts(preferences, remaining)
            val first = remaining.firstOrNull()
            if (first != null) {
                writeActive(preferences, first)
            } else {
                clearActive(preferences)
            }
            next = first
        }
        awaitActiveToken(next?.token)
        return next
    }

    /**
     * The auth interceptor reads [sessionTokenState], which trails DataStore by a
     * dispatch. Wait for it so the first request after a write already carries
     * the new token. Bounded so a concurrent write can't park the caller forever.
     */
    private suspend fun awaitActiveToken(token: String?) {
        withTimeoutOrNull(TOKEN_PROPAGATION_TIMEOUT_MS) {
            sessionTokenState.first { it == token }
        }
    }

    private fun readAccounts(preferences: Preferences): List<StoredAccount> {
        val stored = preferences[ACCOUNTS_KEY]?.let(::decodeAccounts).orEmpty()
        val legacy = activeFromMirror(preferences) ?: return stored
        return if (stored.any { it.userId == legacy.userId }) stored else stored + legacy
    }

    private fun activeFromMirror(preferences: Preferences): StoredAccount? {
        val token = preferences[SESSION_TOKEN_KEY] ?: return null
        val userId = preferences[USER_ID_KEY] ?: return null
        return StoredAccount(
            token = token,
            userId = userId,
            username = preferences[USERNAME_KEY].orEmpty(),
            handle = preferences[USER_HANDLE_KEY].orEmpty(),
            name = preferences[USER_NAME_KEY].orEmpty(),
            avatarUrl = preferences[USER_AVATAR_KEY].orEmpty(),
        )
    }

    private fun writeAccounts(preferences: MutablePreferences, accounts: List<StoredAccount>) {
        if (accounts.isEmpty()) {
            preferences.remove(ACCOUNTS_KEY)
        } else {
            preferences[ACCOUNTS_KEY] = encodeAccounts(accounts)
        }
    }

    private fun writeActive(preferences: MutablePreferences, account: StoredAccount) {
        preferences[SESSION_TOKEN_KEY] = account.token
        preferences[USER_ID_KEY] = account.userId
        preferences[USERNAME_KEY] = account.username
        preferences[USER_HANDLE_KEY] = account.handle
        preferences[USER_NAME_KEY] = account.name
        preferences[USER_AVATAR_KEY] = account.avatarUrl
    }

    private fun clearActive(preferences: MutablePreferences) {
        preferences.remove(SESSION_TOKEN_KEY)
        preferences.remove(USER_ID_KEY)
        preferences.remove(USERNAME_KEY)
        preferences.remove(USER_HANDLE_KEY)
        preferences.remove(USER_NAME_KEY)
        preferences.remove(USER_AVATAR_KEY)
    }

    private fun encodeAccounts(accounts: List<StoredAccount>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("token", account.token)
                    .put("userId", account.userId)
                    .put("username", account.username)
                    .put("handle", account.handle)
                    .put("name", account.name)
                    .put("avatarUrl", account.avatarUrl)
            )
        }
        return array.toString()
    }

    private fun decodeAccounts(json: String): List<StoredAccount> = try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val token = obj.optString("token").takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val userId = obj.optString("userId").takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            StoredAccount(
                token = token,
                userId = userId,
                username = obj.optString("username"),
                handle = obj.optString("handle"),
                name = obj.optString("name"),
                avatarUrl = obj.optString("avatarUrl"),
            )
        }
    } catch (_: JSONException) {
        emptyList()
    }
}
