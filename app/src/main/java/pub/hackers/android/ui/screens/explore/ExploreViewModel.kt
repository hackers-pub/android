package pub.hackers.android.ui.screens.explore

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

enum class ExploreTab {
    LOCAL, GLOBAL
}

data class ExploreUiState(
    val selectedTab: ExploreTab = ExploreTab.LOCAL,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: HackersPubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadTimeline()
    }

    fun selectTab(tab: ExploreTab) {
        if (tab != _uiState.value.selectedTab) {
            _uiState.update {
                it.copy(
                    selectedTab = tab,
                    posts = emptyList(),
                    endCursor = null,
                    hasNextPage = false
                )
            }
            loadTimeline()
        }
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = when (_uiState.value.selectedTab) {
                ExploreTab.LOCAL -> repository.getLocalTimeline()
                ExploreTab.GLOBAL -> repository.getPublicTimeline()
            }

            result
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            posts = data.posts,
                            hasNextPage = data.hasNextPage,
                            endCursor = data.endCursor,
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

            val result = when (_uiState.value.selectedTab) {
                ExploreTab.LOCAL -> repository.getLocalTimeline(refresh = true)
                ExploreTab.GLOBAL -> repository.getPublicTimeline(refresh = true)
            }

            result
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            posts = data.posts,
                            hasNextPage = data.hasNextPage,
                            endCursor = data.endCursor,
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

            val result = when (currentState.selectedTab) {
                ExploreTab.LOCAL -> repository.getLocalTimeline(after = currentState.endCursor)
                ExploreTab.GLOBAL -> repository.getPublicTimeline(after = currentState.endCursor)
            }

            result
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            posts = it.posts + data.posts,
                            hasNextPage = data.hasNextPage,
                            endCursor = data.endCursor,
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
