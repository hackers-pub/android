package pub.hackers.android.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Post
import javax.inject.Inject

data class TimelineUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: HackersPubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadTimeline()
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getPersonalTimeline()
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
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

            repository.getPersonalTimeline(refresh = true)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
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

            repository.getPersonalTimeline(after = currentState.endCursor)
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

    fun sharePost(postId: String) {
        viewModelScope.launch {
            repository.sharePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == postId) {
                                    post.copy(
                                        viewerHasShared = true,
                                        engagementStats = post.engagementStats.copy(
                                            shares = post.engagementStats.shares + 1
                                        )
                                    )
                                } else post
                            }
                        )
                    }
                }
        }
    }

    fun unsharePost(postId: String) {
        viewModelScope.launch {
            repository.unsharePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == postId) {
                                    post.copy(
                                        viewerHasShared = false,
                                        engagementStats = post.engagementStats.copy(
                                            shares = maxOf(0, post.engagementStats.shares - 1)
                                        )
                                    )
                                } else post
                            }
                        )
                    }
                }
        }
    }
}
