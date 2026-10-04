package pub.hackers.android.data.auth

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.apolloStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.local.StoredAccount
import pub.hackers.android.data.messaging.FcmTokenManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Account
import pub.hackers.android.domain.model.Session
import pub.hackers.android.ui.AppViewModel

class AccountSessionCoordinatorTest {

    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val repository = mockk<HackersPubRepository>(relaxed = true)
    private val apolloClient = mockk<ApolloClient>(relaxed = true)
    private val notificationStateManager = mockk<NotificationStateManager>(relaxed = true)
    private val fcmTokenManager = mockk<FcmTokenManager>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val context = mockk<Context> {
        every { getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
    }

    private val coordinator = AccountSessionCoordinator(
        sessionManager = sessionManager,
        repository = repository,
        apolloClient = apolloClient,
        notificationStateManager = notificationStateManager,
        fcmTokenManager = fcmTokenManager,
        workManager = workManager,
        context = context,
    )

    @Before
    fun setUp() {
        // apolloStore is an extension property; stub it so clearAll() is a no-op.
        mockkStatic("com.apollographql.apollo.cache.normalized.NormalizedCache")
        every { apolloClient.apolloStore } returns mockk(relaxed = true)
        every { sessionManager.accounts } returns flowOf(listOf(ALICE, BOB))
    }

    @After
    fun tearDown() {
        unmockkStatic("com.apollographql.apollo.cache.normalized.NormalizedCache")
    }

    @Test
    fun `switchTo moves push registration from the old account to the new one`() = runTest {
        every { sessionManager.userId } returns flowOf(ALICE.userId)
        coEvery { sessionManager.switchAccount(BOB.userId) } returns true

        assertTrue(coordinator.switchTo(BOB.userId))

        coVerifyOrder {
            fcmTokenManager.unregisterCurrentToken()
            notificationStateManager.clear()
            sessionManager.switchAccount(BOB.userId)
            fcmTokenManager.registerCurrentToken()
        }
        verify { notificationManager.cancelAll() }
    }

    @Test
    fun `switchTo the active account does nothing`() = runTest {
        every { sessionManager.userId } returns flowOf(ALICE.userId)

        assertTrue(coordinator.switchTo(ALICE.userId))

        coVerify(exactly = 0) { sessionManager.switchAccount(any()) }
        coVerify(exactly = 0) { fcmTokenManager.unregisterCurrentToken() }
    }

    @Test
    fun `switchTo an unknown account fails without tearing anything down`() = runTest {
        every { sessionManager.userId } returns flowOf(ALICE.userId)

        assertFalse(coordinator.switchTo("missing"))

        coVerify(exactly = 0) { fcmTokenManager.unregisterCurrentToken() }
        coVerify(exactly = 0) { notificationStateManager.clear() }
        coVerify(exactly = 0) { sessionManager.switchAccount(any()) }
    }

    @Test
    fun `signIn while another account is active hands push over to the new account`() = runTest {
        every { sessionManager.userId } returns flowOf(ALICE.userId)

        coordinator.signIn(BOB_SESSION)

        coVerifyOrder {
            fcmTokenManager.unregisterCurrentToken()
            sessionManager.saveSession(
                token = BOB.token,
                userId = BOB.userId,
                username = BOB.username,
                handle = BOB.handle,
                name = BOB.name,
                avatarUrl = BOB.avatarUrl,
            )
            fcmTokenManager.registerCurrentToken()
        }
    }

    @Test
    fun `first signIn leaves push registration to the app`() = runTest {
        every { sessionManager.userId } returns flowOf(null)

        coordinator.signIn(BOB_SESSION)

        coVerify { sessionManager.saveSession(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { fcmTokenManager.unregisterCurrentToken() }
        coVerify(exactly = 0) { fcmTokenManager.registerCurrentToken() }
    }

    @Test
    fun `signOut with another stored account switches to it`() = runTest {
        every { sessionManager.sessionToken } returns flowOf(BOB.token)
        coEvery { sessionManager.clearSession() } returns ALICE

        val next = coordinator.signOut()

        assertEquals(ALICE, next)
        coVerifyOrder {
            fcmTokenManager.unregisterCurrentToken()
            repository.revokeSession(BOB.token)
            sessionManager.clearSession()
            fcmTokenManager.registerCurrentToken()
        }
        verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `signOut of the last account stops notification polling`() = runTest {
        every { sessionManager.sessionToken } returns flowOf(ALICE.token)
        coEvery { sessionManager.clearSession() } returns null

        assertNull(coordinator.signOut())

        verify { workManager.cancelUniqueWork(AppViewModel.NOTIFICATION_WORK_NAME) }
        coVerify(exactly = 0) { fcmTokenManager.registerCurrentToken() }
    }

    private companion object {
        val ALICE = StoredAccount("alice-token", "alice-id", "alice", "@alice@hackers.pub", "Alice", "a.png")
        val BOB = StoredAccount("bob-token", "bob-id", "bob", "@bob@hackers.pub", "Bob", "b.png")
        val BOB_SESSION = Session(
            id = BOB.token,
            account = Account(
                id = BOB.userId,
                username = BOB.username,
                name = BOB.name,
                avatarUrl = BOB.avatarUrl,
                handle = BOB.handle,
            ),
        )
    }
}
