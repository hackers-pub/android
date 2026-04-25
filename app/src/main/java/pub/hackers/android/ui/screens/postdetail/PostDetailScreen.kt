package pub.hackers.android.ui.screens.postdetail

import android.annotation.SuppressLint
import android.content.Intent
import android.text.Html
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.zIndex
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.domain.model.TocItem
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.HtmlContentStyle
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.LinkPreviewCard
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.MediaImage
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.QuotedPostPreview
import pub.hackers.android.ui.components.ReactionPicker
import pub.hackers.android.ui.components.TocList
import pub.hackers.android.ui.components.TocPanel
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit,
    onEditArticleClick: (String) -> Unit = {},
    isLoggedIn: Boolean = true,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val bookmarkedMessage = stringResource(R.string.bookmarked)
    val bookmarkRemovedMessage = stringResource(R.string.bookmark_removed)
    val colors = LocalAppColors.current
    val confirmBeforeDelete by viewModel.preferencesManager.confirmBeforeDelete.collectAsState(
        initial = true
    )
    val confirmBeforeShare by viewModel.preferencesManager.confirmBeforeShare.collectAsState(initial = false)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showShareConfirmation by remember { mutableStateOf(false) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var showTocSheet by remember { mutableStateOf(false) }

    val screenScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val headingCoords = remember(postId) { mutableMapOf<String, LayoutCoordinates>() }
    var bodyItemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val onAnchorClick: (String) -> Unit = remember(postId) {
        { id ->
            val hc = headingCoords[id.substringAfter("--")]
            val ic = bodyItemCoords
            if (hc != null && hc.isAttached && ic != null && ic.isAttached) {
                val offsetInItem = (hc.positionInWindow().y - ic.positionInWindow().y).toInt()
                screenScope.launch {
                    lazyListState.animateScrollToItem(0, offsetInItem.coerceAtLeast(0))
                }
            }
        }
    }
    val onScrollToTop: () -> Unit = remember(lazyListState, screenScope) {
        { screenScope.launch { lazyListState.animateScrollToItem(0, 0) } }
    }
    val tocAvailable = uiState.post?.typename == "Article" && uiState.toc.isNotEmpty()
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                lazyListState.firstVisibleItemScrollOffset > 600
        }
    }

    var activeHeadingId by remember(postId) { mutableStateOf<String?>(null) }
    LaunchedEffect(lazyListState, postId) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (firstIdx, scrollOffset) ->
            val body = bodyItemCoords
            val offsets = if (body != null && body.isAttached) {
                headingCoords.entries.mapNotNull { (id, coords) ->
                    if (!coords.isAttached) null
                    else id to body.localPositionOf(coords, Offset.Zero).y
                }.toMap()
            } else emptyMap()
            val active = computeActiveHeadingId(offsets, firstIdx, scrollOffset)
            if (active != activeHeadingId) activeHeadingId = active
        }
    }

    // Navigate back after successful deletion
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    // WebView bottom sheet
    if (webViewUrl != null) {
        ModalBottomSheet(
            onDismissRequest = { webViewUrl = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            WebViewSheetContent(url = webViewUrl!!)
        }
    }

    if (showTocSheet && tocAvailable) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.table_of_contents),
                    style = LocalAppTypography.current.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TocList(
                    items = uiState.toc,
                    onAnchorClick = { id ->
                        showTocSheet = false
                        onAnchorClick(id)
                    },
                    activeId = activeHeadingId,
                )
            }
        }
    }

    // Reaction picker bottom sheet
    if (uiState.showReactionPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleReactionPicker() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReactionPicker(
                reactionGroups = uiState.reactionGroups,
                isSubmitting = uiState.isReacting,
                onEmojiSelect = { viewModel.toggleReaction(it) },
                onClose = { viewModel.toggleReactionPicker() }
            )
        }
    }

    // Shares bottom sheet
    if (uiState.showSharesSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSharesSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SharesSheet(
                actors = uiState.shareActors,
                isLoading = uiState.isLoadingShares,
                onProfileClick = { handle ->
                    viewModel.dismissSharesSheet()
                    onProfileClick(handle)
                },
                onClose = { viewModel.dismissSharesSheet() }
            )
        }
    }

    // Reactors bottom sheet
    if (uiState.showReactorsSheet && uiState.reactionGroups.isNotEmpty()) {
        val initialIndex = uiState.selectedReactionGroup
            ?.let { selected -> uiState.reactionGroups.indexOf(selected).coerceAtLeast(0) }
            ?: 0
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissReactorsSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReactorsSheet(
                groups = uiState.reactionGroups,
                initialIndex = initialIndex,
                onProfileClick = { handle ->
                    viewModel.dismissReactorsSheet()
                    onProfileClick(handle)
                }
            )
        }
    }

    // Quotes bottom sheet
    if (uiState.showQuotesSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissQuotesSheet() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            QuotesSheet(
                posts = uiState.quotePosts,
                isLoading = uiState.isLoadingQuotes,
                onPostClick = { id ->
                    viewModel.dismissQuotesSheet()
                    onPostClick(id)
                },
                onClose = { viewModel.dismissQuotesSheet() }
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_post_confirm_title)) },
            text = { Text(stringResource(R.string.delete_post_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deletePost()
                    }
                ) {
                    Text(
                        stringResource(R.string.delete_post),
                        color = colors.reaction
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showShareConfirmation) {
        val hasShared = uiState.post?.viewerHasShared == true
        AlertDialog(
            onDismissRequest = { showShareConfirmation = false },
            title = {
                Text(stringResource(if (hasShared) R.string.unshare_confirm_title else R.string.share_confirm_title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareConfirmation = false
                        if (hasShared) viewModel.unsharePost() else viewModel.sharePost()
                    }
                ) {
                    Text(stringResource(if (hasShared) R.string.unshare_confirm_action else R.string.share_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (uiState.deleteError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteError() },
            title = { Text(stringResource(R.string.action_error)) },
            text = { Text(uiState.deleteError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDeleteError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = if (uiState.post?.typename == "Article") stringResource(R.string.article) else "Post",
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.accent
                        )
                    }
                },
                trailingContent = if (tocAvailable || uiState.canDelete || uiState.canEdit) {
                    {
                        if (tocAvailable) {
                            IconButton(onClick = { showTocSheet = true }) {
                                Icon(
                                    imageVector = Icons.Filled.FormatListBulleted,
                                    contentDescription = stringResource(R.string.table_of_contents),
                                    tint = colors.accent,
                                )
                            }
                        }
                        if (uiState.canDelete || uiState.canEdit) {
                            PostDetailActionMenu(
                                isDeleting = uiState.isDeleting,
                                canEdit = uiState.canEdit,
                                canDelete = uiState.canDelete,
                                onEdit = { onEditArticleClick(postId) },
                                onDelete = {
                                    if (confirmBeforeDelete) {
                                        showDeleteConfirmation = true
                                    } else {
                                        viewModel.deletePost()
                                    }
                                }
                            )
                        }
                    }
                } else null
            )
        },
        floatingActionButton = {
            if (uiState.post != null && isLoggedIn) {
                FloatingActionButton(
                    onClick = { onReplyClick(postId) },
                    containerColor = colors.composeAccent,
                    contentColor = colors.composeOnAccent
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(R.string.reply)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
                    .zIndex(1f)
            ) {
                FloatingActionButton(
                    onClick = onScrollToTop,
                    shape = CircleShape,
                    containerColor = colors.surface,
                    contentColor = colors.accent
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.scroll_to_top)
                    )
                }
            }
            val post = uiState.post
            PostDetailStateDispatch(
                post = post,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onRetry = { viewModel.loadPost(postId) },
            ) { resolvedPost ->
                val replies = viewModel.replies.collectAsLazyPagingItems()
                val localReplies by viewModel.locallyAddedReplies.collectAsState()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = {
                        viewModel.refresh()
                        replies.refresh()
                    }
                ) {
                    PostDetailContent(
                        post = resolvedPost,
                        reactionGroups = uiState.reactionGroups,
                        toc = uiState.toc,
                        replies = replies,
                        localReplies = localReplies,
                        lazyListState = lazyListState,
                        headingCoords = headingCoords,
                        onBodyPositioned = { bodyItemCoords = it },
                        onAnchorClick = onAnchorClick,
                        activeHeadingId = activeHeadingId,
                        onProfileClick = onProfileClick,
                        onPostClick = onPostClick,
                        onReplyClick = { onReplyClick(postId) },
                        onShareClick = {
                            if (confirmBeforeShare) {
                                showShareConfirmation = true
                            } else {
                                if (resolvedPost.viewerHasShared) {
                                    viewModel.unsharePost()
                                } else {
                                    viewModel.sharePost()
                                }
                            }
                        },
                        onReactionClick = { group -> viewModel.showReactorsSheet(group) },
                        onReactionPickerClick = { viewModel.toggleReactionPicker() },
                        onBookmarkClick = if (isLoggedIn) {
                            {
                                Toast.makeText(
                                    context,
                                    if (resolvedPost.viewerHasBookmarked) {
                                        bookmarkRemovedMessage
                                    } else {
                                        bookmarkedMessage
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.toggleBookmark()
                            }
                        } else {
                            null
                        },
                        onQuoteClick = { onQuoteClick(postId) },
                        onSharesClick = { viewModel.showSharesSheet() },
                        onQuotesClick = { viewModel.showQuotesSheet() },
                        onReactionsClick = { viewModel.showAllReactors() },
                        onExternalShareClick = {
                            val shareUrl = uiState.post?.url
                                ?: uiState.post?.iri
                            if (shareUrl != null) {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareUrl)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        },
                        onWebViewClick = { url -> webViewUrl = url }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailActionMenu(
    isDeleting: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More actions"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (canEdit) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.edit_article),
                            color = colors.textPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = colors.textPrimary
                        )
                    },
                    onClick = {
                        expanded = false
                        onEdit()
                    }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete_post),
                            color = colors.reaction
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = colors.reaction
                        )
                    },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    enabled = !isDeleting
                )
            }
        }
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun PostDetailContent(
    post: Post,
    reactionGroups: List<ReactionGroup>,
    toc: List<TocItem>,
    replies: LazyPagingItems<Post>,
    lazyListState: LazyListState = rememberLazyListState(),
    headingCoords: MutableMap<String, LayoutCoordinates> = remember(post.id) { mutableMapOf() },
    onBodyPositioned: (LayoutCoordinates) -> Unit = {},
    onAnchorClick: (String) -> Unit = {},
    activeHeadingId: String? = null,
    onProfileClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onShareClick: () -> Unit,
    onReplyClick: () -> Unit,
    onReactionClick: (ReactionGroup) -> Unit,
    onReactionPickerClick: () -> Unit,
    onBookmarkClick: (() -> Unit)?,
    onQuoteClick: () -> Unit,
    onSharesClick: () -> Unit,
    onQuotesClick: () -> Unit,
    onReactionsClick: () -> Unit,
    onExternalShareClick: () -> Unit,
    onWebViewClick: (String) -> Unit = {},
    localReplies: List<Post> = emptyList(),
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val translationFailedText = stringResource(R.string.translation_failed)

    var translatedContent by remember(post.id) { mutableStateOf<String?>(null) }
    var translationError by remember(post.id) { mutableStateOf<String?>(null) }
    var isTranslating by remember(post.id) { mutableStateOf(false) }
    var showTranslated by remember(post.id) { mutableStateOf(false) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            .withZone(ZoneId.systemDefault())
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = navBarBottom + 96.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .onGloballyPositioned(onBodyPositioned)
            ) {
                // Reply target preview
                val replyTarget = post.replyTarget
                if (replyTarget != null) {
                    ReplyTargetPreview(
                        post = replyTarget,
                        onClick = { onPostClick(replyTarget.id) },
                        onProfileClick = onProfileClick
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { onPostClick(replyTarget.id) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.replying_to)} @${replyTarget.actor.handle}",
                            style = typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = post.actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(AppShapes.avatarTimeline)
                            .clip(CircleShape)
                            .clickable { onProfileClick(post.actor.handle) },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.clickable { onProfileClick(post.actor.handle) }
                    ) {
                        pub.hackers.android.ui.components.RichDisplayName(
                            name = post.actor.name,
                            fallback = post.actor.handle,
                            style = typography.bodyLargeSemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = post.actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isArticle = post.typename == "Article"

                post.name?.let { title ->
                    Text(
                        text = title,
                        style = if (isArticle) typography.titleLarge else typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(if (isArticle) 12.dp else 8.dp))
                    if (isArticle) {
                        HorizontalDivider(color = colors.divider)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                val onHeadingPositioned: (String, LayoutCoordinates) -> Unit =
                    remember(post.id) {
                        { id, coords -> headingCoords[id] = coords }
                    }

                if (isArticle && toc.isNotEmpty() && !showTranslated) {
                    TocPanel(
                        items = toc,
                        onAnchorClick = onAnchorClick,
                        activeId = activeHeadingId,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val translatedText = translatedContent
                if (showTranslated && translatedText != null) {
                    Text(
                        text = translatedText,
                        style = typography.bodyLarge,
                        color = colors.textBody,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    HtmlContent(
                        html = post.content,
                        modifier = Modifier.fillMaxWidth(),
                        contentStyle = HtmlContentStyle.Prose,
                        onMentionClick = onProfileClick,
                        onHeadingPositioned = if (isArticle) onHeadingPositioned else null,
                    )
                }

                if (isTranslating) {
                    Text(
                        text = stringResource(R.string.translating),
                        style = typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val translationErrorText = translationError
                if (translationErrorText != null) {
                    Text(
                        text = translationErrorText,
                        style = typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!isTranslating && translationError == null) {
                    // Read Configuration from composition state so locale changes
                    // trigger recomposition instead of serving stale values.
                    val configuration = LocalConfiguration.current
                    Text(
                        text = if (showTranslated) stringResource(R.string.show_original) else stringResource(
                            R.string.translate
                        ),
                        style = typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable {
                                if (showTranslated) {
                                    showTranslated = false
                                    return@clickable
                                }
                                translatedContent?.let {
                                    showTranslated = true
                                    return@clickable
                                }
                                val targetLanguageTag = androidx.core.os.ConfigurationCompat
                                    .getLocales(configuration)
                                    .get(0)?.language ?: Locale.getDefault().language
                                scope.launch {
                                    isTranslating = true
                                    translationError = null
                                    try {
                                        val translated = translateDetailContent(
                                            html = post.content,
                                            targetLanguageTag = targetLanguageTag
                                        )
                                        translatedContent = translated
                                        showTranslated = true
                                    } catch (_: Exception) {
                                        translationError = translationFailedText
                                    } finally {
                                        isTranslating = false
                                    }
                                }
                            }
                    )
                }

                if (post.media.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MediaCarousel(media = post.media)
                }

                if (post.media.isEmpty() && post.quotedPost == null && post.link != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinkPreviewCard(
                        link = post.link,
                        onProfileClick = onProfileClick
                    )
                }

                if (post.quotedPost != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    QuotedPostPreview(
                        post = post.quotedPost,
                        onClick = { onPostClick(post.quotedPost.id) },
                        onProfileClick = onProfileClick
                    )
                }

                if (post.typename == "Article" && post.url != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onWebViewClick(post.url) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.read_on_web))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateFormatter.format(post.published),
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )

                    if (post.visibility != pub.hackers.android.domain.model.PostVisibility.PUBLIC) {
                        Text(
                            text = "\u00B7",
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Icon(
                            imageVector = when (post.visibility) {
                                pub.hackers.android.domain.model.PostVisibility.UNLISTED -> Icons.Outlined.Lock
                                pub.hackers.android.domain.model.PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                                pub.hackers.android.domain.model.PostVisibility.DIRECT -> Icons.Outlined.Lock
                                else -> Icons.Filled.Public
                            },
                            contentDescription = when (post.visibility) {
                                pub.hackers.android.domain.model.PostVisibility.UNLISTED -> "Unlisted"
                                pub.hackers.android.domain.model.PostVisibility.FOLLOWERS -> "Followers only"
                                pub.hackers.android.domain.model.PostVisibility.DIRECT -> "Direct"
                                else -> "Public"
                            },
                            modifier = Modifier.size(14.dp),
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "${post.engagementStats.replies} ${stringResource(R.string.replies)}",
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${post.engagementStats.shares} ${stringResource(R.string.shares)}",
                        style = typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable { onSharesClick() }
                    )
                    Text(
                        text = "${post.engagementStats.reactions} ${stringResource(R.string.reactions)}",
                        style = typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable { onReactionsClick() }
                    )
                    Text(
                        text = "${post.engagementStats.quotes} ${stringResource(R.string.quotes)}",
                        style = typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable { onQuotesClick() }
                    )
                }

                if (reactionGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        reactionGroups.forEach { group ->
                            Card(
                                onClick = { onReactionClick(group) },
                                shape = RoundedCornerShape(AppShapes.reactionPillRadius),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (group.viewerHasReacted)
                                        colors.accent.copy(alpha = 0.2f)
                                    else
                                        colors.surface
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (group.emoji != null) {
                                        Text(text = group.emoji)
                                    } else if (group.customEmoji != null) {
                                        AsyncImage(
                                            model = group.customEmoji.imageUrl,
                                            contentDescription = group.customEmoji.name,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = group.count.toString(),
                                        style = typography.labelMedium,
                                        color = if (group.viewerHasReacted)
                                            colors.accent
                                        else
                                            colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = colors.divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = onReplyClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = stringResource(R.string.reply),
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.share),
                            tint = if (post.viewerHasShared)
                                colors.share
                            else
                                colors.textSecondary
                        )
                    }
                    IconButton(onClick = onReactionPickerClick) {
                        Icon(
                            imageVector = Icons.Outlined.AddReaction,
                            contentDescription = stringResource(R.string.reactions),
                            tint = if (reactionGroups.any { it.viewerHasReacted })
                                colors.accent
                            else
                                colors.textSecondary
                        )
                    }
                    if (onBookmarkClick != null) {
                        IconButton(onClick = onBookmarkClick) {
                            Icon(
                                imageVector = if (post.viewerHasBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(R.string.bookmark),
                                tint = if (post.viewerHasBookmarked) colors.bookmark else colors.textSecondary
                            )
                        }
                    }
                    IconButton(onClick = onQuoteClick) {
                        Icon(
                            imageVector = Icons.Outlined.FormatQuote,
                            contentDescription = stringResource(R.string.quotes),
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onExternalShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = colors.divider)
            }
        }

        if (replies.itemCount > 0 || localReplies.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.replies),
                    style = typography.bodyLargeSemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            items(
                count = replies.itemCount,
                key = replies.itemKey { it.id }
            ) { index ->
                val reply = replies[index] ?: return@items
                PostCard(
                    post = reply,
                    onClick = { onPostClick(reply.id) },
                    onProfileClick = onProfileClick,
                    onQuotedPostClick = onPostClick
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
            }

            if (replies.loadState.append is LoadState.Loading) {
                item {
                    LoadingItem()
                }
            }

            items(
                items = localReplies,
                key = { reply -> "local-${reply.id}" }
            ) { reply ->
                PostCard(
                    post = reply,
                    onClick = { onPostClick(reply.id) },
                    onProfileClick = onProfileClick,
                    onQuotedPostClick = onPostClick
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.divider)
            }
        }
    }
}

@Composable
private fun ReactorsSheet(
    groups: List<ReactionGroup>,
    initialIndex: Int,
    onProfileClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val safeInitial = initialIndex.coerceIn(0, (groups.size - 1).coerceAtLeast(0))
    var selectedIndex by remember(groups) { mutableIntStateOf(safeInitial) }
    val selectedGroup = groups.getOrNull(selectedIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.reactors),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (groups.isEmpty() || selectedGroup == null) {
            Text(
                text = stringResource(R.string.no_reactors),
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEachIndexed { index, group ->
                val isSelected = index == selectedIndex
                Card(
                    onClick = { selectedIndex = index },
                    shape = RoundedCornerShape(AppShapes.reactionPillRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            colors.accent.copy(alpha = 0.2f)
                        else
                            colors.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (group.emoji != null) {
                            Text(text = group.emoji)
                        } else if (group.customEmoji != null) {
                            AsyncImage(
                                model = group.customEmoji.imageUrl,
                                contentDescription = group.customEmoji.name,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = group.count.toString(),
                            style = typography.labelMedium,
                            color = if (isSelected) colors.accent else colors.textPrimary
                        )
                    }
                }
            }
        }

        if (selectedGroup.reactors.isEmpty()) {
            Text(
                text = stringResource(R.string.no_reactors),
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            selectedGroup.reactors.forEach { actor ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick(actor.handle) }
                        .padding(vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        pub.hackers.android.ui.components.RichDisplayName(
                            name = actor.name,
                            fallback = actor.handle,
                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharesSheet(
    actors: List<pub.hackers.android.domain.model.Actor>,
    isLoading: Boolean,
    onProfileClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.shares),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (actors.isEmpty()) {
            Text(
                text = "No shares yet",
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            actors.forEach { actor ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick(actor.handle) }
                        .padding(vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        pub.hackers.android.ui.components.RichDisplayName(
                            name = actor.name,
                            fallback = actor.handle,
                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotesSheet(
    posts: List<Post>,
    isLoading: Boolean,
    onPostClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.quotes),
            style = typography.bodyLargeSemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (posts.isEmpty()) {
            Text(
                text = "No quotes yet",
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            posts.forEach { post ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPostClick(post.id) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = post.actor.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(AppShapes.avatarQuoted)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            pub.hackers.android.ui.components.RichDisplayName(
                                name = post.actor.name,
                                fallback = post.actor.handle,
                                style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                                emojiHeight = 14.dp
                            )
                            Text(
                                text = post.actor.handle,
                                style = typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HtmlContent(
                        html = post.content,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HorizontalDivider(
                    color = colors.divider,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ReplyTargetPreview(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick(post.actor.handle) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                pub.hackers.android.ui.components.RichDisplayName(
                    name = post.actor.name,
                    fallback = post.actor.handle,
                    style = typography.bodyLargeSemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = post.actor.handle,
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HtmlContent(
            html = post.content,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            onMentionClick = onProfileClick,
            onTextClick = onClick
        )

        if (post.media.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            MediaCarousel(media = post.media)
        }
    }
}

@Composable
private fun MediaCarousel(media: List<pub.hackers.android.domain.model.Media>) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    if (media.size == 1) {
        MediaImage(
            url = media[0].url,
            alt = media[0].alt,
            isVideo = media[0].isVideo,
            thumbnailUrl = media[0].thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(AppShapes.mediaRadius))
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            media.forEach { item ->
                MediaImage(
                    url = item.url,
                    alt = item.alt,
                    isVideo = item.isVideo,
                    thumbnailUrl = item.thumbnailUrl,
                    modifier = Modifier
                        .height(250.dp)
                        .width(300.dp)
                        .clip(RoundedCornerShape(AppShapes.mediaRadius))
                )
            }
        }
    }

    if (media.size > 1) {
        Text(
            text = "${media.size} images",
            style = typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private suspend fun translateDetailContent(
    html: String,
    targetLanguageTag: String
): String = withContext(Dispatchers.IO) {
    val plainText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    if (plainText.isBlank()) return@withContext ""

    val languageIdentifier = LanguageIdentification.getClient()
    val detectedTag = languageIdentifier.identifyLanguage(plainText).await()
    val sourceLanguage = TranslateLanguage.fromLanguageTag(detectedTag)
    val targetLanguage = TranslateLanguage.fromLanguageTag(targetLanguageTag)

    if (sourceLanguage == null || targetLanguage == null || sourceLanguage == targetLanguage) {
        languageIdentifier.close()
        return@withContext plainText
    }

    val options = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLanguage)
        .setTargetLanguage(targetLanguage)
        .build()
    val translator = Translation.getClient(options)
    try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        translator.translate(plainText).await()
    } finally {
        translator.close()
        languageIdentifier.close()
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun PostDetailStateDispatch(
    post: Post?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    content: @Composable (Post) -> Unit,
) {
    when {
        isLoading -> {
            FullScreenLoading()
        }

        error != null -> {
            ErrorMessage(
                message = error,
                onRetry = onRetry,
            )
        }

        post != null -> {
            content(post)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun WebViewSheetContent(url: String) {
    var pageTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
    ) {
        Text(
            text = pageTitle.ifEmpty { url },
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    setOnTouchListener { v, event ->
                        // Prevent the bottom sheet from intercepting scroll events
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: android.graphics.Bitmap?
                        ) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            pageTitle = title ?: ""
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }

                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Last heading whose top has scrolled to or above the viewport top is "active".
 * Offsets are in pixels relative to the body item's top; scroll offsets come
 * from [androidx.compose.foundation.lazy.LazyListState].
 */
internal fun computeActiveHeadingId(
    headingOffsetsInBody: Map<String, Float>,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): String? {
    if (headingOffsetsInBody.isEmpty()) return null
    val scrollPast = if (firstVisibleItemIndex > 0) {
        Float.MAX_VALUE
    } else {
        firstVisibleItemScrollOffset.toFloat()
    }
    return headingOffsetsInBody.entries
        .filter { (_, y) -> y <= scrollPast }
        .maxByOrNull { (_, y) -> y }
        ?.key
}
