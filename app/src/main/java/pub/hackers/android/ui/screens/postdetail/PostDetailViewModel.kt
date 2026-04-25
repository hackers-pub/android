package pub.hackers.android.ui.screens.postdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.paging.cursorPager
import pub.hackers.android.data.paging.distinctByEffectiveId
import pub.hackers.android.data.paging.postRepliesPage
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.domain.model.TocItem
import pub.hackers.android.ui.bookmark.BookmarkMutationCoordinator
import pub.hackers.android.ui.screens.compose.ReplyPostedSignal
import javax.inject.Inject

data class PostDetailUiState(
    val post: Post? = null,
    val reactionGroups: List<ReactionGroup> = emptyList(),
    val toc: List<TocItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val canDelete: Boolean = false,
    val canEdit: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val isDeleted: Boolean = false,
    val isReacting: Boolean = false,
    val showReactionPicker: Boolean = false,
    val showSharesSheet: Boolean = false,
    val shareActors: List<Actor> = emptyList(),
    val isLoadingShares: Boolean = false,
    val showQuotesSheet: Boolean = false,
    val quotePosts: List<Post> = emptyList(),
    val isLoadingQuotes: Boolean = false,
    val showReactorsSheet: Boolean = false,
    val selectedReactionGroup: ReactionGroup? = null,
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    private val sessionManager: SessionManager,
    val preferencesManager: PreferencesManager,
    private val replyPostedSignal: ReplyPostedSignal,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()
    private val bookmarkCoordinator = BookmarkMutationCoordinator(
        scope = viewModelScope,
        requestMutation = { targetPostId, shouldBookmark ->
            if (shouldBookmark) repository.bookmarkPost(targetPostId)
            else repository.unbookmarkPost(targetPostId)
        },
        applyDesiredState = { targetPostId, isBookmarked ->
            _uiState.update { state ->
                val post = state.post
                if (post == null || post.id != targetPostId) state
                else state.copy(post = post.copy(viewerHasBookmarked = isBookmarked))
            }
        },
        revertFailedState = { targetPostId, attemptedState ->
            _uiState.update { state ->
                val post = state.post
                if (post == null || post.id != targetPostId) state
                else state.copy(post = post.copy(viewerHasBookmarked = !attemptedState))
            }
        },
    )

    // Locally-composed replies appended optimistically after a successful reply
    // from this screen. Rendered after the paginated replies; cleared on refresh
    // so the server-authoritative list wins.
    private val _locallyAddedReplies = MutableStateFlow<List<Post>>(emptyList())
    val locallyAddedReplies: StateFlow<List<Post>> = _locallyAddedReplies.asStateFlow()

    // Replies are paginated independently of the main post payload. The main
    // post, reactionGroups, and sheet/delete/translation state stay in UiState
    // because they are single-instance optimistic updates that don't benefit
    // from the PagingData overlay pattern.
    val replies: Flow<PagingData<Post>> =
        cursorPager { after -> repository.postRepliesPage(postId, after) }
            .flow
            .distinctByEffectiveId()
            .cachedIn(viewModelScope)

    init {
        loadPost(postId)
        viewModelScope.launch {
            replyPostedSignal.events.collect { event ->
                if (event.replyTargetId == postId) {
                    appendLocalReply(event.reply)
                }
            }
        }
    }

    private fun appendLocalReply(reply: Post) {
        _locallyAddedReplies.update { it + reply }
        _uiState.update { state ->
            val post = state.post ?: return@update state
            state.copy(
                post = post.copy(
                    engagementStats = post.engagementStats.copy(
                        replies = post.engagementStats.replies + 1
                    )
                )
            )
        }
    }

    fun loadPost(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getPostDetail(id)
                .onSuccess { result ->
                    val viewerHandle = sessionManager.userHandle.first()
                    val isOwner = viewerHandle != null &&
                        result.post.actor.handle.equals(viewerHandle, ignoreCase = true) &&
                        result.post.sharedPost == null
                    val canEdit = isOwner &&
                        result.post.typename == "Article" &&
                        pub.hackers.android.FeatureFlags.ARTICLE_EDIT_ENABLED

                    _uiState.update {
                        it.copy(
                            post = result.post,
                            reactionGroups = result.reactionGroups,
                            toc = result.toc,
                            isLoading = false,
                            canDelete = isOwner,
                            canEdit = canEdit
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
        _locallyAddedReplies.value = emptyList()
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            repository.getPostDetail(postId)
                .onSuccess { result ->
                    val viewerHandle = sessionManager.userHandle.first()
                    val isOwner = viewerHandle != null &&
                        result.post.actor.handle.equals(viewerHandle, ignoreCase = true) &&
                        result.post.sharedPost == null
                    val canEdit = isOwner &&
                        result.post.typename == "Article" &&
                        pub.hackers.android.FeatureFlags.ARTICLE_EDIT_ENABLED

                    _uiState.update {
                        it.copy(
                            post = result.post,
                            reactionGroups = result.reactionGroups,
                            toc = result.toc,
                            isRefreshing = false,
                            canDelete = isOwner,
                            canEdit = canEdit
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false) }
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

    fun deletePost() {
        if (_uiState.value.isDeleting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }

            repository.deletePost(postId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isDeleting = false, deleteError = error.message)
                    }
                }
        }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    fun toggleReactionPicker() {
        _uiState.update { it.copy(showReactionPicker = !it.showReactionPicker) }
    }

    fun showSharesSheet() {
        _uiState.update { it.copy(showSharesSheet = true, isLoadingShares = true) }
        viewModelScope.launch {
            repository.getPostShares(postId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(shareActors = result.actors, isLoadingShares = false)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingShares = false) }
                }
        }
    }

    fun dismissSharesSheet() {
        _uiState.update { it.copy(showSharesSheet = false) }
    }

    fun showQuotesSheet() {
        _uiState.update { it.copy(showQuotesSheet = true, isLoadingQuotes = true) }
        viewModelScope.launch {
            repository.getPostQuotes(postId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(quotePosts = result.posts, isLoadingQuotes = false)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingQuotes = false) }
                }
        }
    }

    fun dismissQuotesSheet() {
        _uiState.update { it.copy(showQuotesSheet = false) }
    }

    fun showReactorsSheet(group: ReactionGroup) {
        _uiState.update {
            it.copy(showReactorsSheet = true, selectedReactionGroup = group)
        }
    }

    fun showAllReactors() {
        _uiState.update {
            it.copy(showReactorsSheet = true, selectedReactionGroup = null)
        }
    }

    fun dismissReactorsSheet() {
        _uiState.update {
            it.copy(showReactorsSheet = false, selectedReactionGroup = null)
        }
    }

    fun toggleBookmark() {
        val post = _uiState.value.post ?: return
        bookmarkCoordinator.toggle(post.id, post.viewerHasBookmarked)
    }

    fun toggleReaction(emoji: String) {
        if (_uiState.value.isReacting) return

        val existingGroup = _uiState.value.reactionGroups.find { it.emoji == emoji }
        val viewerHasReacted = existingGroup?.viewerHasReacted == true

        viewModelScope.launch {
            _uiState.update { it.copy(isReacting = true) }

            val result = if (viewerHasReacted) {
                repository.removeReactionFromPost(postId, emoji)
            } else {
                repository.addReactionToPost(postId, emoji)
            }

            result
                .onSuccess {
                    // Optimistically update local state
                    _uiState.update { state ->
                        val updatedGroups = if (viewerHasReacted) {
                            state.reactionGroups.map { group ->
                                if (group.emoji == emoji) {
                                    group.copy(
                                        count = maxOf(0, group.count - 1),
                                        viewerHasReacted = false
                                    )
                                } else group
                            }.filter { it.count > 0 || it.viewerHasReacted }
                        } else {
                            val existing = state.reactionGroups.find { it.emoji == emoji }
                            if (existing != null) {
                                state.reactionGroups.map { group ->
                                    if (group.emoji == emoji) {
                                        group.copy(
                                            count = group.count + 1,
                                            viewerHasReacted = true
                                        )
                                    } else group
                                }
                            } else {
                                state.reactionGroups + ReactionGroup(
                                    emoji = emoji,
                                    customEmoji = null,
                                    count = 1,
                                    reactors = emptyList(),
                                    viewerHasReacted = true
                                )
                            }
                        }

                        val totalReactions = updatedGroups.sumOf { it.count }

                        state.copy(
                            reactionGroups = updatedGroups,
                            isReacting = false,
                            post = state.post?.copy(
                                engagementStats = state.post.engagementStats.copy(
                                    reactions = totalReactions
                                )
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isReacting = false) }
                }
        }
    }
}
