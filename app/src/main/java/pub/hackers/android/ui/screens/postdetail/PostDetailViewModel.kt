package pub.hackers.android.ui.screens.postdetail

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
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import javax.inject.Inject

data class PostDetailUiState(
    val post: Post? = null,
    val reactionGroups: List<ReactionGroup> = emptyList(),
    val replies: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    init {
        loadPost(postId)
    }

    fun loadPost(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getPostDetail(id)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            post = result.post,
                            reactionGroups = result.reactionGroups,
                            replies = result.replies,
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

    fun sharePost() {
        viewModelScope.launch {
            repository.sharePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.post?.let { post ->
                            state.copy(
                                post = post.copy(
                                    viewerHasShared = true,
                                    engagementStats = post.engagementStats.copy(
                                        shares = post.engagementStats.shares + 1
                                    )
                                )
                            )
                        } ?: state
                    }
                }
        }
    }

    fun unsharePost() {
        viewModelScope.launch {
            repository.unsharePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.post?.let { post ->
                            state.copy(
                                post = post.copy(
                                    viewerHasShared = false,
                                    engagementStats = post.engagementStats.copy(
                                        shares = maxOf(0, post.engagementStats.shares - 1)
                                    )
                                )
                            )
                        } ?: state
                    }
                }
        }
    }
}
