package pub.hackers.android.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Notification
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: HackersPubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getNotifications()
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            notifications = result.notifications,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            repository.getNotifications(refresh = true)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            notifications = result.notifications,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (!currentState.hasNextPage || currentState.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            repository.getNotifications(after = currentState.endCursor)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications + result.notifications,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }
}
