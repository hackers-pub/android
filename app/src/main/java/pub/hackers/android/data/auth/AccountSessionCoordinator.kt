package pub.hackers.android.data.auth

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.apolloStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.local.StoredAccount
import pub.hackers.android.data.messaging.FcmTokenManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Session
import pub.hackers.android.ui.AppViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns every change of the active account: signing in (including adding a
 * second account), switching between stored accounts, and signing out.
 *
 * Changing accounts is more than swapping the token. Push registration is tied
 * to the session that registered it, notification bookkeeping and the Apollo
 * normalized cache hold viewer-specific data (`viewerHasShared`, bookmarks,
 * reactions), so all of that is torn down for the old account and set up again
 * for the new one.
 */
@Singleton
class AccountSessionCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: HackersPubRepository,
    private val apolloClient: ApolloClient,
    private val notificationStateManager: NotificationStateManager,
    private val fcmTokenManager: FcmTokenManager,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context,
) {

    /**
     * Stores [session] and makes it active. When another account was active, it
     * stays stored so the user can switch back to it later.
     */
    suspend fun signIn(session: Session) {
        val previousUserId = sessionManager.userId.first()
        val replacesActiveAccount = previousUserId != null && previousUserId != session.account.id
        if (replacesActiveAccount) {
            detachActiveAccount()
        }
        sessionManager.saveSession(
            token = session.id,
            userId = session.account.id,
            username = session.account.username,
            handle = session.account.handle,
            name = session.account.name,
            avatarUrl = session.account.avatarUrl,
        )
        // A first sign-in (false → true) registers FCM from HackersPubApp; when
        // the device was already signed in that transition never fires.
        if (replacesActiveAccount) {
            attachActiveAccount()
        }
    }

    /**
     * Makes the stored account with [userId] active. Returns `false` when no such
     * account is stored; switching to the account that is already active is a
     * successful no-op.
     */
    suspend fun switchTo(userId: String): Boolean {
        if (sessionManager.userId.first() == userId) return true
        if (sessionManager.accounts.first().none { it.userId == userId }) return false

        detachActiveAccount()
        val switched = sessionManager.switchAccount(userId)
        attachActiveAccount()
        return switched
    }

    /**
     * Revokes and forgets the active account. Returns the account that became
     * active in its place, or `null` when none is left and the device is now
     * signed out.
     */
    suspend fun signOut(): StoredAccount? {
        val sessionId = sessionManager.sessionToken.first()
        detachActiveAccount()
        if (sessionId != null) {
            repository.revokeSession(sessionId)
        }
        val next = sessionManager.clearSession()
        clearCache()
        if (next != null) {
            fcmTokenManager.registerCurrentToken()
        } else {
            workManager.cancelUniqueWork(AppViewModel.NOTIFICATION_WORK_NAME)
        }
        return next
    }

    /** Runs while the outgoing account's token is still the active one. */
    private suspend fun detachActiveAccount() {
        fcmTokenManager.unregisterCurrentToken()
        notificationStateManager.clear()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    /** Runs once the incoming account's token is active. */
    private suspend fun attachActiveAccount() {
        clearCache()
        fcmTokenManager.registerCurrentToken()
    }

    private suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            apolloClient.apolloStore.clearAll()
        }
    }
}
