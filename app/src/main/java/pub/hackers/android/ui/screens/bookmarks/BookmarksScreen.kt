package pub.hackers.android.ui.screens.bookmarks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import pub.hackers.android.R
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.ReactionPicker
import pub.hackers.android.ui.share.sharePlainText
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val items = viewModel.posts.collectAsLazyPagingItems()
    val newerPosts by viewModel.newerPosts.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val bookmarkedMessage = stringResource(R.string.bookmarked)
    val bookmarkRemovedMessage = stringResource(R.string.bookmark_removed)
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val visiblePostCount = newerPosts.size + items.itemCount

    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

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
                title = stringResource(R.string.bookmarks),
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = colors.accent
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BookmarkTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (tab) {
                                BookmarkTab.ALL -> stringResource(R.string.all)
                                BookmarkTab.ARTICLES -> stringResource(R.string.articles)
                                BookmarkTab.NOTES -> stringResource(R.string.notes)
                            },
                            style = if (isSelected) typography.bodyLargeSemiBold else typography.bodyLarge,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (isSelected) colors.accent else colors.surface)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = colors.divider,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                val refresh = items.loadState.refresh
                when {
                    refresh is LoadState.Error && visiblePostCount == 0 -> {
                        ErrorMessage(
                            message = refresh.error.message ?: stringResource(R.string.error_generic),
                            onRetry = { items.refresh() }
                        )
                    }

                    visiblePostCount == 0 && refresh is LoadState.NotLoading -> {
                        BookmarksEmptyState(
                            onRefresh = { items.refresh() }
                        )
                    }

                    refresh is LoadState.Loading && visiblePostCount == 0 -> {
                        FullScreenLoading()
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = refresh is LoadState.Loading || uiState.isLoadingNewer,
                            onRefresh = {
                                if (visiblePostCount > 0) viewModel.loadNewerPosts()
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
                                            onReplyClick(post.sharedPost?.id ?: post.id)
                                        },
                                        onShareClick = {
                                            val targetId = post.sharedPost?.id ?: post.id
                                            if (post.viewerHasShared) {
                                                viewModel.unsharePost(targetId)
                                            } else {
                                                viewModel.sharePost(targetId)
                                            }
                                        },
                                        onQuoteClick = {
                                            onQuoteClick(post.sharedPost?.id ?: post.id)
                                        },
                                        onEditClick = { onEditClick(it.id) },
                                        onReactionClick = {
                                            viewModel.toggleFavourite(post)
                                        },
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
                                    key = items.itemKey { it.sharedPost?.id ?: it.id }
                                ) { index ->
                                    val post = items[index] ?: return@items
                                    PostCard(
                                        post = post,
                                        onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                                        onProfileClick = onProfileClick,
                                        onReplyClick = {
                                            onReplyClick(post.sharedPost?.id ?: post.id)
                                        },
                                        onShareClick = {
                                            val targetId = post.sharedPost?.id ?: post.id
                                            if (post.viewerHasShared) {
                                                viewModel.unsharePost(targetId)
                                            } else {
                                                viewModel.sharePost(targetId)
                                            }
                                        },
                                        onQuoteClick = {
                                            onQuoteClick(post.sharedPost?.id ?: post.id)
                                        },
                                        onEditClick = { onEditClick(it.id) },
                                        onReactionClick = {
                                            viewModel.toggleFavourite(post)
                                        },
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

@Composable
private fun BookmarksEmptyState(
    onRefresh: () -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = colors.bookmark,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_bookmarks),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_bookmarks_description),
            style = typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        androidx.compose.material3.Button(onClick = onRefresh) {
            Text(stringResource(R.string.refresh))
        }
    }
}
