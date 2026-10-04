package pub.hackers.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Real DataStore on real dispatchers: SessionManager waits for its token
// StateFlow to catch up with each write, which virtual time would short-circuit.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            tempFolder.newFile("session.preferences_pb")
        }
        sessionManager = SessionManager(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `adding a second account keeps the first and activates the new one`() = runBlocking {
        sessionManager.save(ALICE)
        sessionManager.save(BOB)

        assertEquals(listOf(ALICE, BOB), sessionManager.accounts.first())
        assertEquals(BOB.userId, sessionManager.userId.first())
        assertEquals(BOB.token, sessionManager.sessionTokenState.value)
    }

    @Test
    fun `signing in again to a stored account replaces its token in place`() = runBlocking {
        sessionManager.save(ALICE)
        sessionManager.save(BOB)
        val refreshed = ALICE.copy(token = "alice-token-2")

        sessionManager.save(refreshed)

        assertEquals(listOf(refreshed, BOB), sessionManager.accounts.first())
        assertEquals(refreshed.token, sessionManager.sessionTokenState.value)
    }

    @Test
    fun `switchAccount activates a stored account`() = runBlocking {
        sessionManager.save(ALICE)
        sessionManager.save(BOB)

        assertTrue(sessionManager.switchAccount(ALICE.userId))

        assertEquals(ALICE.userId, sessionManager.userId.first())
        assertEquals(ALICE.handle, sessionManager.userHandle.first())
        assertEquals(ALICE.token, sessionManager.sessionTokenState.value)
        assertEquals(listOf(ALICE, BOB), sessionManager.accounts.first())
    }

    @Test
    fun `switchAccount to an unknown account changes nothing`() = runBlocking {
        sessionManager.save(ALICE)

        assertFalse(sessionManager.switchAccount("missing"))

        assertEquals(ALICE.userId, sessionManager.userId.first())
    }

    @Test
    fun `clearSession promotes the next stored account`() = runBlocking {
        sessionManager.save(ALICE)
        sessionManager.save(BOB)

        val next = sessionManager.clearSession()

        assertEquals(ALICE, next)
        assertEquals(listOf(ALICE), sessionManager.accounts.first())
        assertEquals(ALICE.token, sessionManager.sessionTokenState.value)
        assertTrue(sessionManager.isLoggedIn.first())
    }

    @Test
    fun `clearSession on the last account signs the device out`() = runBlocking {
        sessionManager.save(ALICE)

        val next = sessionManager.clearSession()

        assertNull(next)
        assertEquals(emptyList<StoredAccount>(), sessionManager.accounts.first())
        assertFalse(sessionManager.isLoggedIn.first())
        assertNull(sessionManager.sessionTokenState.value)
    }

    @Test
    fun `a pre-multi-account session is picked up and kept when adding another`() = runBlocking {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("session_token")] = ALICE.token
            prefs[stringPreferencesKey("user_id")] = ALICE.userId
            prefs[stringPreferencesKey("username")] = ALICE.username
            prefs[stringPreferencesKey("user_handle")] = ALICE.handle
            prefs[stringPreferencesKey("user_name")] = ALICE.name
            prefs[stringPreferencesKey("user_avatar")] = ALICE.avatarUrl
        }

        assertEquals(listOf(ALICE), sessionManager.accounts.first())

        sessionManager.save(BOB)

        assertEquals(listOf(ALICE, BOB), sessionManager.accounts.first())
    }

    private suspend fun SessionManager.save(account: StoredAccount) = saveSession(
        token = account.token,
        userId = account.userId,
        username = account.username,
        handle = account.handle,
        name = account.name,
        avatarUrl = account.avatarUrl,
    )

    private companion object {
        val ALICE = StoredAccount(
            token = "alice-token",
            userId = "alice-id",
            username = "alice",
            handle = "@alice@hackers.pub",
            name = "Alice",
            avatarUrl = "https://hackers.pub/alice.png",
        )
        val BOB = StoredAccount(
            token = "bob-token",
            userId = "bob-id",
            username = "bob",
            handle = "@bob@hackers.pub",
            name = "Bob",
            avatarUrl = "https://hackers.pub/bob.png",
        )
    }
}
