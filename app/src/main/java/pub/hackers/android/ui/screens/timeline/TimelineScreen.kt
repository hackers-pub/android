package pub.hackers.android.ui.screens.timeline

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.ui.HomeFeed
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HomeFeedSelector
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.LanguageFilterRow
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.ReactionPicker
import pub.hackers.android.ui.share.sharePlainText
import pub.hackers.android.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onComposeClick: (String?) -> Unit,
    onQuoteClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit,
    onRecommendedActorsClick: () -> Unit = {},
    onComposeArticleClick: () -> Unit = {},
    onComposeArticleLongClick: () -> Unit = {},
    selectedHomeFeed: HomeFeed = HomeFeed.TIMELINE,
    onHomeFeedSelected: (HomeFeed) -> Unit = {},
    tabRetapped: Long = 0L,
    showLanguageFilter: Boolean = false,
    userAvatarUrl: String? = null,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = viewModel.posts.collectAsLazyPagingItems()
    val newerPosts by viewModel.newerPosts.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val bookmarkedMessage = stringResource(R.string.bookmarked)
    val bookmarkRemovedMessage = stringResource(R.string.bookmark_removed)
    val colors = LocalAppColors.current

    // A language selected before the filter row was hidden would otherwise keep filtering invisibly.
    LaunchedEffect(showLanguageFilter) {
        if (!showLanguageFilter) viewModel.selectLanguage(null)
    }

    // Refresh draft count when screen becomes visible (e.g., returning from Drafts)
    LaunchedEffect(Unit) {
        viewModel.loadDraftCount()
    }

    // After composing a new post, scroll to top (ViewModel handles cache invalidation).
    val refreshAt by viewModel.refreshTrigger.refreshAt.collectAsState()
    LaunchedEffect(refreshAt) {
        if (refreshAt > 0L) {
            listState.scrollToItem(0)
        }
    }

    // Home tab re-tap: if at top, refresh; otherwise scroll to top.
    LaunchedEffect(tabRetapped) {
        if (tabRetapped > 0L) {
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                if (items.itemCount > 0) viewModel.loadNewerPosts()
                else items.refresh()
            } else {
                listState.animateScrollToItem(0)
            }
        }
    }

    // Reaction picker bottom sheet — look up the currently-loaded post by id.
    val pickerPostId = uiState.reactionPickerPostId
    if (pickerPostId != null) {
        val pickerPost = (newerPosts + items.itemSnapshotList.items).find {
            it.id == pickerPostId || it.sharedPost?.id == pickerPostId
        }
        val targetPost = pickerPost?.sharedPost ?: pickerPost
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideReactionPicker() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReactionPicker(
                reactionGroups = targetPost?.reactionGroups ?: emptyList(),
                isSubmitting = false,
                onEmojiSelect = { emoji ->
                    pickerPost?.let { viewModel.toggleReaction(it, emoji) }
                },
                onClose = { viewModel.hideReactionPicker() }
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = stringResource(R.string.personal_timeline),
                titleContent = {
                    HomeFeedSelector(
                        selectedFeed = selectedHomeFeed,
                        onFeedSelected = onHomeFeedSelected,
                    )
                },
            ) {
                // New article button with draft badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = colors.surface, shape = CircleShape)
                        .combinedClickable(
                            onClick = { onComposeArticleClick() },
                            onLongClick = { onComposeArticleLongClick() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (uiState.draftCount > 0) {
                                Badge { Text(uiState.draftCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = stringResource(R.string.new_article),
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Recommended actors button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = colors.surface, shape = CircleShape)
                        .clickable { onRecommendedActorsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = stringResource(R.string.recommended_actors),
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings gear icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = colors.surface, shape = CircleShape)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // User avatar (if logged in)
                if (userAvatarUrl != null) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onComposeClick(null) },
                containerColor = colors.composeAccent,
                contentColor = colors.composeOnAccent
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.compose)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showLanguageFilter) {
                LanguageFilterRow(
                    languages = uiState.suggestedFilterLanguages,
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = viewModel::selectLanguage,
                )
                HorizontalDivider(color = colors.divider)
            }

            Box(modifier = Modifier.weight(1f)) {
                val refresh = items.loadState.refresh
                when {
                    refresh is LoadState.Error && items.itemCount == 0 -> {
                        ErrorMessage(
                            message = refresh.error.message ?: stringResource(R.string.error_generic),
                            onRetry = { items.refresh() }
                        )
                    }

                    items.itemCount == 0 && refresh is LoadState.NotLoading && refresh.endOfPaginationReached -> {
                        ErrorMessage(
                            message = stringResource(R.string.no_posts),
                            onRefresh = { items.refresh() }
                        )
                    }

                    items.itemCount == 0 -> {
                        // Initial load in progress (Loading, or pre-Loading NotLoading frame).
                        FullScreenLoading()
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = refresh is LoadState.Loading || uiState.isLoadingNewer,
                            onRefresh = {
                                if (items.itemCount > 0) viewModel.loadNewerPosts()
                                else items.refresh()
                            }
                        ) {
                            LazyColumn(state = listState) {
                                items(
                                    count = newerPosts.size,
                                    key = { index ->
                                        val post = newerPosts[index]
                                        "newer-${post.sharedPost?.id ?: post.id}"
                                    }
                                ) { index ->
                                    val post = newerPosts[index]
                                    PostCard(
                                        post = post,
                                        onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                                        onProfileClick = onProfileClick,
                                        onReplyClick = {
                                            onComposeClick(post.sharedPost?.id ?: post.id)
                                        },
                                        onShareClick = {
                                            if (post.viewerHasShared) {
                                                viewModel.unsharePost(post.id)
                                            } else {
                                                viewModel.sharePost(post.id)
                                            }
                                        },
                                        onQuoteClick = { onQuoteClick(post.sharedPost?.id ?: post.id) },
                                        onEditClick = { onEditClick(it.id) },
                                        onReactionClick = { viewModel.toggleFavourite(post) },
                                        onReactionLongPress = {
                                            viewModel.showReactionPicker(post.sharedPost?.id ?: post.id)
                                        },
                                        onBookmarkClick = {
                                            val displayPost = post.sharedPost ?: post
                                            Toast.makeText(
                                                context,
                                                if (displayPost.viewerHasBookmarked) {
                                                    bookmarkRemovedMessage
                                                } else {
                                                    bookmarkedMessage
                                                },
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            viewModel.toggleBookmark(post)
                                        },
                                        onPinClick = { viewModel.togglePin(it) },
                                        onExternalShare = { context.sharePlainText(it) },
                                        onQuotedPostClick = onPostClick,
                                        onVotePoll = viewModel::voteOnPoll
                                    )
                                    HorizontalDivider(
                                        color = colors.divider,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                items(
                                    count = items.itemCount,
                                    key = items.itemKey { it.id }
                                ) { index ->
                                    val post = items[index] ?: return@items
                                    PostCard(
                                        post = post,
                                        onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                                        onProfileClick = onProfileClick,
                                        onReplyClick = {
                                            onComposeClick(post.sharedPost?.id ?: post.id)
                                        },
                                        onShareClick = {
                                            if (post.viewerHasShared) {
                                                viewModel.unsharePost(post.id)
                                            } else {
                                                viewModel.sharePost(post.id)
                                            }
                                        },
                                        onQuoteClick = { onQuoteClick(post.sharedPost?.id ?: post.id) },
                                        onEditClick = { onEditClick(it.id) },
                                        onReactionClick = { viewModel.toggleFavourite(post) },
                                        onReactionLongPress = {
                                            viewModel.showReactionPicker(post.sharedPost?.id ?: post.id)
                                        },
                                        onBookmarkClick = {
                                            val displayPost = post.sharedPost ?: post
                                            Toast.makeText(
                                                context,
                                                if (displayPost.viewerHasBookmarked) {
                                                    bookmarkRemovedMessage
                                                } else {
                                                    bookmarkedMessage
                                                },
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            viewModel.toggleBookmark(post)
                                        },
                                        onPinClick = { viewModel.togglePin(it) },
                                        onExternalShare = { context.sharePlainText(it) },
                                        onQuotedPostClick = onPostClick,
                                        onVotePoll = viewModel::voteOnPoll
                                    )
                                    HorizontalDivider(
                                        color = colors.divider,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                if (items.loadState.append is LoadState.Loading) {
                                    item { LoadingItem() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
