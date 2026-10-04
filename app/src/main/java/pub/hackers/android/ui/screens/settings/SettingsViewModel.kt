package pub.hackers.android.ui.screens.settings
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.apolloStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import pub.hackers.android.data.auth.AccountSessionCoordinator
import pub.hackers.android.data.auth.PasskeyManager
import pub.hackers.android.data.local.StoredAccount
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Passkey
import pub.hackers.android.ui.theme.ThemeMode
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val userName: String? = null,
    val userHandle: String? = null,
    val userAvatar: String? = null,
    val appVersion: String = "",
    val cacheSize: String = "",
    val accounts: List<StoredAccount> = emptyList(),
    val activeUserId: String? = null,
    val isChangingAccount: Boolean = false,
    val message: String? = null,
    val confirmBeforeDelete: Boolean = true,
    val confirmBeforeShare: Boolean = false,
    val timelineMaxLength: Int = 0,
    val useInAppBrowser: Boolean = true,
    val fontSizePercent: Int = 100,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showLanguageFilter: Boolean = false,
    val passkeys: List<Passkey> = emptyList(),
    val accountId: String? = null,
    val isLoadingPasskeys: Boolean = false,
    val isRegisteringPasskey: Boolean = false
)

sealed interface SettingsEvent {
    /** The last stored account was signed out; the device is now signed out. */
    data object SignedOut : SettingsEvent

    /** A different stored account is now active (switched to, or promoted after sign-out). */
    data object AccountSwitched : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val preferencesManager: PreferencesManager,
    private val repository: HackersPubRepository,
    private val apolloClient: ApolloClient,
    private val passkeyManager: PasskeyManager,
    private val accountSessionCoordinator: AccountSessionCoordinator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        loadUserInfo()
        observeAccounts()
        loadAppVersion()
        loadPreferences()
        calculateCacheSize()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val name = sessionManager.userName.first()
            val handle = sessionManager.userHandle.first()
            val avatar = sessionManager.userAvatar.first()

            _uiState.update {
                it.copy(
                    userName = name,
                    userHandle = handle,
                    userAvatar = avatar
                )
            }
        }
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            combine(sessionManager.accounts, sessionManager.userId, ::Pair)
                .collect { (accounts, activeUserId) ->
                    _uiState.update {
                        it.copy(accounts = accounts, activeUserId = activeUserId)
                    }
                }
        }
    }

    fun switchAccount(userId: String) {
        if (_uiState.value.isChangingAccount || userId == _uiState.value.activeUserId) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingAccount = true) }
            val switched = accountSessionCoordinator.switchTo(userId)
            _uiState.update { it.copy(isChangingAccount = false) }
            if (switched) {
                _events.send(SettingsEvent.AccountSwitched)
            }
        }
    }

    private fun loadAppVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = packageInfo.versionName ?: "Unknown"
            // PackageInfo.longVersionCode is API 28+. PackageInfoCompat covers
            // minSdk 26 by falling back to the deprecated versionCode on older
            // devices.
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            _uiState.update { it.copy(appVersion = "$version ($versionCode)") }
        } catch (_: Exception) {
            _uiState.update { it.copy(appVersion = "Unknown") }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val confirmDelete = preferencesManager.confirmBeforeDelete.first()
            val confirmShare = preferencesManager.confirmBeforeShare.first()
            val maxLength = preferencesManager.timelineMaxLength.first()
            val inAppBrowser = preferencesManager.useInAppBrowser.first()
            val fontSize = preferencesManager.fontSizePercent.first()
            val theme = preferencesManager.themeMode.first()
            val showLanguageFilter = preferencesManager.showLanguageFilter.first()

            _uiState.update {
                it.copy(
                    confirmBeforeDelete = confirmDelete,
                    confirmBeforeShare = confirmShare,
                    timelineMaxLength = maxLength,
                    useInAppBrowser = inAppBrowser,
                    fontSizePercent = fontSize,
                    themeMode = theme,
                    showLanguageFilter = showLanguageFilter
                )
            }
        }
    }

    fun setConfirmBeforeDelete(value: Boolean) {
        _uiState.update { it.copy(confirmBeforeDelete = value) }
        viewModelScope.launch { preferencesManager.setConfirmBeforeDelete(value) }
    }

    fun setConfirmBeforeShare(value: Boolean) {
        _uiState.update { it.copy(confirmBeforeShare = value) }
        viewModelScope.launch { preferencesManager.setConfirmBeforeShare(value) }
    }

    fun setTimelineMaxLength(value: Int) {
        _uiState.update { it.copy(timelineMaxLength = value) }
        viewModelScope.launch { preferencesManager.setTimelineMaxLength(value) }
    }

    fun setUseInAppBrowser(value: Boolean) {
        _uiState.update { it.copy(useInAppBrowser = value) }
        viewModelScope.launch { preferencesManager.setUseInAppBrowser(value) }
    }

    fun setFontSizePercent(value: Int) {
        _uiState.update { it.copy(fontSizePercent = value) }
        viewModelScope.launch { preferencesManager.setFontSizePercent(value) }
    }

    fun setThemeMode(value: ThemeMode) {
        _uiState.update { it.copy(themeMode = value) }
        viewModelScope.launch { preferencesManager.setThemeMode(value) }
    }

    fun setShowLanguageFilter(value: Boolean) {
        _uiState.update { it.copy(showLanguageFilter = value) }
        viewModelScope.launch { preferencesManager.setShowLanguageFilter(value) }
    }

    fun signOut() {
        if (_uiState.value.isChangingAccount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingAccount = true) }
            val next = accountSessionCoordinator.signOut()
            _uiState.update { it.copy(isChangingAccount = false) }
            _events.send(
                if (next != null) SettingsEvent.AccountSwitched else SettingsEvent.SignedOut
            )
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            val cacheDir = context.cacheDir
            val size = withContext(Dispatchers.IO) {
                getDirSize(cacheDir)
            }
            _uiState.update { it.copy(cacheSize = formatBytes(size)) }
        }
    }

    private fun getDirSize(dir: java.io.File): Long {
        var size = 0L
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        }
        return size
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format(Locale.ROOT, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apolloClient.apolloStore.clearAll()
                    // Also clear file cache
                    context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                }
                calculateCacheSize()
                _uiState.update { it.copy(message = "Cache cleared") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to clear cache") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun loadPasskeys() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPasskeys = true) }
            repository.getPasskeys()
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            passkeys = result.passkeys,
                            accountId = result.accountId,
                            isLoadingPasskeys = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingPasskeys = false) }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun registerPasskey(name: String, context: Context) {
        val accountId = _uiState.value.accountId ?: run {
            android.util.Log.e("PasskeyAuth", "registerPasskey: accountId is null")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRegisteringPasskey = true) }
            try {
                android.util.Log.d("PasskeyAuth", "registerPasskey: getting options for accountId=$accountId")
                val optionsJson = repository.getPasskeyRegistrationOptions(accountId)
                    .getOrThrow()
                android.util.Log.d("PasskeyAuth", "registerPasskey: got options")

                val registrationResponse = passkeyManager.register(optionsJson, context)
                android.util.Log.d("PasskeyAuth", "registerPasskey: got registration response: ${registrationResponse.take(200)}")

                // Parse and re-serialize to ensure clean JSON, then convert to Map for Apollo
                val jsonObj = org.json.JSONObject(registrationResponse)
                android.util.Log.d("PasskeyAuth", "registerPasskey: JSON keys: ${jsonObj.keys().asSequence().toList()}")
                android.util.Log.d("PasskeyAuth", "registerPasskey: response.keys: ${org.json.JSONObject(jsonObj.getString("response")).keys().asSequence().toList()}")

                val responseMap = jsonToMap(jsonObj)
                android.util.Log.d("PasskeyAuth", "registerPasskey: verifying with server, name=$name, map keys=${responseMap.keys}")

                val result = repository.verifyPasskeyRegistration(accountId, name, responseMap)
                    .getOrThrow()
                android.util.Log.d("PasskeyAuth", "registerPasskey: verified=${result.verified}")

                if (result.verified) {
                    _uiState.update {
                        it.copy(
                            message = "Passkey registered",
                            isRegisteringPasskey = false
                        )
                    }
                    loadPasskeys()
                } else {
                    _uiState.update {
                        it.copy(
                            message = "Passkey registration failed",
                            isRegisteringPasskey = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PasskeyAuth", "registerPasskey: failed", e)
                _uiState.update {
                    it.copy(
                        message = e.message ?: "Passkey registration failed",
                        isRegisteringPasskey = false
                    )
                }
            }
        }
    }

    fun revokePasskey(passkeyId: String) {
        viewModelScope.launch {
            repository.revokePasskey(passkeyId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            passkeys = state.passkeys.filter { it.id != passkeyId },
                            message = "Passkey removed"
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(message = "Failed to remove passkey") }
                }
        }
    }

    private fun jsonToMap(json: org.json.JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            map[key] = when (value) {
                is org.json.JSONObject -> jsonToMap(value)
                is org.json.JSONArray -> jsonToList(value)
                org.json.JSONObject.NULL -> null
                else -> value
            }
        }
        return map
    }

    private fun jsonToList(array: org.json.JSONArray): List<Any?> {
        return (0 until array.length()).map { i ->
            when (val value = array.get(i)) {
                is org.json.JSONObject -> jsonToMap(value)
                is org.json.JSONArray -> jsonToList(value)
                org.json.JSONObject.NULL -> null
                else -> value
            }
        }
    }
}
