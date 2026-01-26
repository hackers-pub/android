package pub.hackers.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.Post
import javax.inject.Inject

data class ProfileUiState(
    val actor: Actor? = null,
    val bio: String? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val handle: String = checkNotNull(savedStateHandle["handle"])

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile(handle)
    }

    fun loadProfile(handle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getProfile(handle)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            actor = result.actor,
                            bio = result.bio,
                            posts = result.posts,
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

            repository.getProfile(handle)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            actor = result.actor,
                            bio = result.bio,
                            posts = result.posts,
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

            repository.getProfile(handle, postsAfter = currentState.endCursor)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            posts = it.posts + result.posts,
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
