package pub.hackers.android.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.R
import pub.hackers.android.data.auth.AccountSessionCoordinator
import pub.hackers.android.data.auth.PasskeyManager
import pub.hackers.android.data.repository.HackersPubRepository
import java.util.UUID
import javax.inject.Inject

enum class SignInStep {
    USERNAME,
    VERIFICATION
}

data class SignInUiState(
    val step: SignInStep = SignInStep.USERNAME,
    val username: String = "",
    val code: String = "",
    val challengeToken: String? = null,
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    private val accountSessionCoordinator: AccountSessionCoordinator,
    private val passkeyManager: PasskeyManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun updateCode(code: String) {
        _uiState.update { it.copy(code = code) }
    }

    fun sendVerificationCode() {
        val username = _uiState.value.username.trim()
        if (username.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.loginByUsername(username)
                .onSuccess { challenge ->
                    _uiState.update {
                        it.copy(
                            step = SignInStep.VERIFICATION,
                            challengeToken = challenge.token,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun verifyCode() {
        val token = _uiState.value.challengeToken ?: return
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.completeLoginChallenge(token, code)
                .onSuccess { session ->
                    accountSessionCoordinator.signIn(session)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun verifyWithDeepLink(token: String, code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.completeLoginChallenge(token, code)
                .onSuccess { session ->
                    accountSessionCoordinator.signIn(session)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun signInWithPasskey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val sessionId = UUID.randomUUID().toString()
                android.util.Log.d("PasskeyAuth", "Step 1: Getting options for sessionId=$sessionId")

                val optionsJson = repository.getPasskeyAuthenticationOptions(sessionId)
                    .getOrThrow()
                android.util.Log.d("PasskeyAuth", "Step 2: Got options: $optionsJson")

                val authResponse = passkeyManager.authenticate(optionsJson)
                android.util.Log.d("PasskeyAuth", "Step 3: Got auth response: $authResponse")

                val authResponseMap = jsonToMap(org.json.JSONObject(authResponse))
                android.util.Log.d("PasskeyAuth", "Step 4: Sending loginByPasskey")

                val session = repository.loginByPasskey(sessionId, authResponseMap)
                    .getOrThrow()
                android.util.Log.d("PasskeyAuth", "Step 5: Login success, session=${session.id}")

                accountSessionCoordinator.signIn(session)
                _uiState.update { it.copy(isLoading = false, isSignedIn = true) }
            } catch (e: NoCredentialException) {
                android.util.Log.w("PasskeyAuth", "No passkey registered", e)
                _uiState.update {
                    it.copy(
                        error = context.getString(R.string.no_passkey_registered),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PasskeyAuth", "Passkey auth failed", e)
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Passkey authentication failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun goBackToUsername() {
        _uiState.update {
            it.copy(
                step = SignInStep.USERNAME,
                code = "",
                challengeToken = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
