package pub.hackers.android.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.repository.HackersPubRepository
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
    private val sessionManager: SessionManager
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
                    sessionManager.saveSession(
                        token = session.id,
                        userId = session.account.id,
                        username = session.account.username,
                        handle = session.account.handle,
                        name = session.account.name,
                        avatarUrl = session.account.avatarUrl
                    )
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
}
