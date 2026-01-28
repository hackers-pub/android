package pub.hackers.android.ui.screens.compose

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
import pub.hackers.android.domain.model.PostVisibility
import javax.inject.Inject

data class ComposeUiState(
    val content: String = "",
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val replyToId: String? = null,
    val replyTargetPost: Post? = null,
    val isLoadingReplyTarget: Boolean = false,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val repository: HackersPubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private var viewerHandle: String? = null

    init {
        viewModelScope.launch {
            repository.getViewer().onSuccess { viewer ->
                viewerHandle = viewer?.handle
            }
        }
    }

    fun setReplyTarget(postId: String) {
        _uiState.update { it.copy(replyToId = postId, isLoadingReplyTarget = true) }
        viewModelScope.launch {
            repository.getPostDetail(postId)
                .onSuccess { result ->
                    val post = result.post
                    val mentionPrefix = buildMentionPrefix(post)
                    _uiState.update {
                        it.copy(
                            replyTargetPost = post,
                            isLoadingReplyTarget = false,
                            content = mentionPrefix
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingReplyTarget = false) }
                }
        }
    }

    private fun buildMentionPrefix(post: Post): String {
        val mentions = mutableSetOf<String>()

        // Add the post author
        mentions.add(post.actor.handle)

        // Add existing mentions from the post
        mentions.addAll(post.mentions)

        // Remove viewer's own handle if present
        viewerHandle?.let { mentions.remove(it) }

        return if (mentions.isNotEmpty()) {
            mentions.joinToString(" ") { "@$it" } + " "
        } else {
            ""
        }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun updateVisibility(visibility: PostVisibility) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun post() {
        val state = _uiState.value
        if (state.content.isBlank() || state.isPosting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }

            repository.createNote(
                content = state.content,
                visibility = state.visibility,
                replyTargetId = state.replyToId
            )
                .onSuccess {
                    _uiState.update { it.copy(isPosting = false, isPosted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isPosting = false
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
