package pub.hackers.android.ui.screens.settings

import android.content.Context
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.data.auth.AccountSessionCoordinator
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.local.StoredAccount
import pub.hackers.android.testutil.MainDispatcherRule
import pub.hackers.android.ui.theme.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val activeUserId = MutableStateFlow<String?>(ALICE.userId)
    private val sessionManager = mockk<SessionManager>(relaxed = true) {
        every { accounts } returns flowOf(listOf(ALICE, BOB))
        every { userId } returns activeUserId
        every { userName } returns flowOf(ALICE.name)
        every { userHandle } returns flowOf(ALICE.handle)
        every { userAvatar } returns flowOf(ALICE.avatarUrl)
    }
    private val preferencesManager = mockk<PreferencesManager> {
        every { confirmBeforeDelete } returns flowOf(true)
        every { confirmBeforeShare } returns flowOf(false)
        every { timelineMaxLength } returns flowOf(0)
        every { useInAppBrowser } returns flowOf(true)
        every { fontSizePercent } returns flowOf(100)
        every { themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { showLanguageFilter } returns flowOf(false)
    }
    private val coordinator = mockk<AccountSessionCoordinator>(relaxed = true)

    private fun newViewModel() = SettingsViewModel(
        sessionManager = sessionManager,
        preferencesManager = preferencesManager,
        repository = mockk(relaxed = true),
        apolloClient = mockk(relaxed = true),
        passkeyManager = mockk(relaxed = true),
        accountSessionCoordinator = coordinator,
        context = mockk<Context>(relaxed = true),
    )

    @Test
    fun `exposes stored accounts and the active one`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        assertEquals(listOf(ALICE, BOB), vm.uiState.value.accounts)
        assertEquals(ALICE.userId, vm.uiState.value.activeUserId)
    }

    @Test
    fun `switchAccount emits AccountSwitched once the coordinator succeeds`() = runTest {
        coEvery { coordinator.switchTo(BOB.userId) } returns true
        val vm = newViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.switchAccount(BOB.userId)
            assertEquals(SettingsEvent.AccountSwitched, awaitItem())
        }
        assertFalse(vm.uiState.value.isChangingAccount)
    }

    @Test
    fun `switchAccount to the active account is ignored`() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()

        vm.switchAccount(ALICE.userId)
        advanceUntilIdle()

        coVerify(exactly = 0) { coordinator.switchTo(any()) }
    }

    @Test
    fun `signOut with another stored account emits AccountSwitched`() = runTest {
        coEvery { coordinator.signOut() } returns BOB
        val vm = newViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.signOut()
            assertEquals(SettingsEvent.AccountSwitched, awaitItem())
        }
    }

    @Test
    fun `signOut of the last account emits SignedOut`() = runTest {
        coEvery { coordinator.signOut() } returns null
        val vm = newViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.signOut()
            assertEquals(SettingsEvent.SignedOut, awaitItem())
        }
    }

    private companion object {
        val ALICE = StoredAccount("alice-token", "alice-id", "alice", "@alice@hackers.pub", "Alice", "a.png")
        val BOB = StoredAccount("bob-token", "bob-id", "bob", "@bob@hackers.pub", "Bob", "b.png")
    }
}
