package pub.hackers.android.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import pub.hackers.android.R
import pub.hackers.android.domain.model.AccountLink
import pub.hackers.android.domain.model.ActorField
import pub.hackers.android.domain.model.Poll
import pub.hackers.android.domain.model.Post
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.share.sharePlainText
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    handle: String,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    onQuoteClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    refreshSignal: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    androidx.compose.runtime.LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.refresh()
            onRefreshConsumed()
        }
    }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val colors = LocalAppColors.current
    var showRssSheet by remember { mutableStateOf(false) }

    val activeFlow: Flow<PagingData<Post>> = remember(selectedTab, viewModel) {
        when (selectedTab) {
            ProfileTab.POSTS -> viewModel.postsTab
            ProfileTab.NOTES -> viewModel.notesTab
            ProfileTab.ARTICLES -> viewModel.articlesTab
        }
    }
    val items = activeFlow.collectAsLazyPagingItems()

    if (showRssSheet) {
        val actor = uiState.actor
        if (actor != null) {
            val username = actor.handle.trimStart('@').substringBefore("@")
            val host = actor.handle.trimStart('@').substringAfter("@")
            RssFeedBottomSheet(
                host = host,
                username = username,
                onDismiss = { showRssSheet = false }
            )
        }
    }

    if (uiState.actionError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissActionError() },
            title = { Text(stringResource(R.string.action_error)) },
            text = { Text(uiState.actionError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissActionError() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = "",
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.accent
                        )
                    }
                },
                trailingContent = {
                    val actor = uiState.actor
                    if (actor != null) {
                        if (actor.handle.endsWith("@hackers.pub")) {
                            IconButton(onClick = { showRssSheet = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.RssFeed,
                                    contentDescription = "RSS Feed",
                                    tint = colors.accent
                                )
                            }
                        }
                        IconButton(onClick = {
                            val profileHandle = actor.handle
                            val normalizedHandle =
                                if (profileHandle.startsWith("@")) profileHandle else "@$profileHandle"
                            context.sharePlainText("https://hackers.pub/$normalizedHandle")
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = colors.accent
                            )
                        }
                    }
                    if (!uiState.isViewer && actor != null) {
                        ProfileActionMenu(
                            followsViewer = uiState.followsViewer,
                            viewerBlocks = uiState.viewerBlocks,
                            isPerformingAction = uiState.isPerformingAction,
                            onRemoveFollower = { viewModel.removeFollower() },
                            onBlock = { viewModel.blockActor() },
                            onUnblock = { viewModel.unblockActor() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val actor = uiState.actor
            ProfileStateDispatch(
                actor = actor,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onRetry = { viewModel.loadProfile() },
            ) { resolvedActor ->
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = {
                        viewModel.refresh()
                        items.refresh()
                    }
                ) {
                    LazyColumn(state = listState) {
                        item {
                            ProfileHeader(
                                avatarUrl = resolvedActor.avatarUrl,
                                name = resolvedActor.name,
                                handle = resolvedActor.handle,
                                bio = uiState.bio,
                                fields = uiState.fields,
                                accountLinks = uiState.accountLinks,
                                isViewer = uiState.isViewer,
                                viewerFollows = uiState.viewerFollows,
                                followsViewer = uiState.followsViewer,
                                viewerBlocks = uiState.viewerBlocks,
                                isPerformingAction = uiState.isPerformingAction,
                                onFollowClick = { viewModel.followActor() },
                                onUnfollowClick = { viewModel.unfollowActor() },
                                onEditProfileClick = onEditProfileClick,
                                onMentionClick = onProfileClick
                            )
                        }

                        item {
                            ProfileTabBar(
                                selectedTab = selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }

                        if (selectedTab == ProfileTab.POSTS && uiState.pinnedPosts.isNotEmpty()) {
                            item {
                                PinnedPostsSection(
                                    posts = uiState.pinnedPosts,
                                    onPostClick = onPostClick,
                                    onProfileClick = onProfileClick,
                                    onReplyClick = onReplyClick,
                                    onQuoteClick = onQuoteClick,
                                    onEditClick = onEditClick,
                                    onPinClick = { viewModel.togglePin(it) },
                                    onExternalShare = { context.sharePlainText(it) },
                                    onVotePoll = viewModel::voteOnPoll
                                )
                            }
                        }

                        val refresh = items.loadState.refresh
                        when {
                            refresh is LoadState.Error && items.itemCount == 0 -> {
                                item {
                                    ErrorMessage(
                                        message = refresh.error.message
                                            ?: stringResource(R.string.error_generic),
                                        onRetry = { items.refresh() }
                                    )
                                }
                            }

                            items.itemCount == 0 && refresh is LoadState.NotLoading && refresh.endOfPaginationReached -> {
                                item {
                                    ErrorMessage(
                                        message = stringResource(R.string.no_posts),
                                        onRefresh = { items.refresh() }
                                    )
                                }
                            }

                            items.itemCount == 0 && refresh is LoadState.Loading -> {
                                item { LoadingItem() }
                            }

                            else -> {
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
                                            onReplyClick(
                                                post.sharedPost?.id ?: post.id
                                            )
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
                                            onQuoteClick(
                                                post.sharedPost?.id ?: post.id
                                            )
                                        },
                                        onEditClick = { onEditClick(it.id) },
                                        onPinClick = { viewModel.togglePin(it) },
                                        onReactionClick = null,
                                        onExternalShare = { context.sharePlainText(it) },
                                        onQuotedPostClick = onPostClick,
                                        onVotePoll = viewModel::voteOnPoll
                                    )
                                    HorizontalDivider(
                                        color = LocalAppColors.current.divider,
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
private fun PinnedPostsSection(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onPinClick: (Post) -> Unit,
    onExternalShare: (String) -> Unit,
    onVotePoll: (suspend (questionId: String, optionIndices: List<Int>) -> Result<Poll>)? = null,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier.padding(top = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.pinned_posts),
                style = typography.labelMedium,
                color = colors.textSecondary
            )
        }

        posts.forEach { post ->
            PostCard(
                post = post,
                onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                onProfileClick = onProfileClick,
                onReplyClick = { onReplyClick(post.sharedPost?.id ?: post.id) },
                onShareClick = null,
                onQuoteClick = { onQuoteClick(post.sharedPost?.id ?: post.id) },
                onReactionClick = null,
                onEditClick = { onEditClick(it.id) },
                onPinClick = onPinClick,
                onExternalShare = onExternalShare,
                onQuotedPostClick = onPostClick,
                onVotePoll = onVotePoll,
            )
            HorizontalDivider(
                color = colors.divider,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ProfileActionMenu(
    followsViewer: Boolean,
    viewerBlocks: Boolean,
    isPerformingAction: Boolean,
    onRemoveFollower: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More actions",
                tint = colors.accent
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (followsViewer) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.remove_follower),
                            color = colors.reaction
                        )
                    },
                    onClick = {
                        expanded = false
                        onRemoveFollower()
                    },
                    enabled = !isPerformingAction
                )
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(if (viewerBlocks) R.string.unblock else R.string.block),
                        color = if (!viewerBlocks) colors.reaction else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    expanded = false
                    if (viewerBlocks) onUnblock() else onBlock()
                },
                enabled = !isPerformingAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RssFeedBottomSheet(
    host: String,
    username: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val clipboardManager = context.getSystemService(
        android.content.Context.CLIPBOARD_SERVICE
    ) as android.content.ClipboardManager

    fun copyFeedUrl(feedPath: String) {
        clipboardManager.setPrimaryClip(
            android.content.ClipData.newPlainText("RSS Feed URL", feedPath)
        )
        android.widget.Toast.makeText(
            context,
            "Feed URL copied to clipboard",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "RSS Feed",
                style = typography.bodyLargeSemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RssFeed,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Posts",
                        style = typography.bodyLargeSemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$host/@$username/feed.xml",
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = { copyFeedUrl("$host/@$username/feed.xml") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RssFeed,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Articles Only",
                        style = typography.bodyLargeSemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$host/@$username/feed.xml?articles",
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = { copyFeedUrl("$host/@$username/feed.xml?articles") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTabBar(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(modifier = Modifier.fillMaxWidth()) {
        ProfileTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (tab) {
                        ProfileTab.POSTS -> stringResource(R.string.profile_tab_posts)
                        ProfileTab.NOTES -> stringResource(R.string.profile_tab_notes)
                        ProfileTab.ARTICLES -> stringResource(R.string.profile_tab_articles)
                    },
                    style = if (isSelected) typography.bodyLargeSemiBold else typography.bodyLarge,
                    color = if (isSelected) colors.accent else colors.textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .then(
                            if (isSelected) Modifier.background(colors.accent) else Modifier
                        )
                )
            }
        }
    }

    HorizontalDivider(
        color = colors.divider,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun ProfileHeader(
    avatarUrl: String,
    name: String?,
    handle: String,
    bio: String?,
    fields: List<ActorField>,
    accountLinks: List<AccountLink>,
    isViewer: Boolean,
    viewerFollows: Boolean,
    followsViewer: Boolean,
    viewerBlocks: Boolean,
    isPerformingAction: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onMentionClick: (String) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(AppShapes.avatarProfile)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        pub.hackers.android.ui.components.RichDisplayName(
            name = name,
            fallback = handle,
            style = typography.titleMedium,
            color = colors.textPrimary,
            emojiHeight = 22.dp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = handle,
            style = typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        if (!isViewer && (followsViewer || viewerBlocks)) {
            Spacer(modifier = Modifier.height(6.dp))
            RelationshipTags(
                followsViewer = followsViewer,
                viewerBlocks = viewerBlocks
            )
        }

        if (isViewer) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onEditProfileClick,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, colors.buttonOutline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.accent
                )
            ) {
                Text(
                    text = stringResource(R.string.edit_profile),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        if (!isViewer) {
            Spacer(modifier = Modifier.height(12.dp))

            if (viewerFollows) {
                OutlinedButton(
                    onClick = onUnfollowClick,
                    enabled = !isPerformingAction,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, colors.buttonOutline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.accent
                    )
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.unfollow),
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    enabled = !isPerformingAction,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    )
                ) {
                    if (isPerformingAction) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.follow),
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                }
            }
        }

        if (!bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            HtmlContent(
                html = bio,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                onMentionClick = onMentionClick
            )
        }

        if (accountLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileAttachments(accountLinks = accountLinks)
        } else if (fields.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileFields(fields = fields)
        }
    }
}

@Composable
private fun ProfileAttachments(accountLinks: List<AccountLink>) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val uriHandler = LocalUriHandler.current
    val borderColor = colors.divider

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        accountLinks.forEachIndexed { index, link ->
            if (index > 0) {
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(link.url) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = link.name,
                    style = typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = link.handle ?: compactUrl(link.url),
                    style = typography.bodyMedium,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (link.verified != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.profile_verified),
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF22C55E)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileFields(fields: List<ActorField>) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val borderColor = colors.divider

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        fields.forEachIndexed { index, field ->
            if (index > 0) {
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = field.name,
                    style = typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                HtmlContent(
                    html = field.value,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

private fun compactUrl(url: String): String {
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
}

@Composable
private fun RelationshipTags(
    followsViewer: Boolean,
    viewerBlocks: Boolean
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (followsViewer) {
            Text(
                text = stringResource(R.string.follows_you),
                style = typography.caption,
                color = colors.accentMuted,
                modifier = Modifier
                    .background(
                        colors.accentMuted.copy(alpha = 0.10f),
                        RoundedCornerShape(AppShapes.tagRadius)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (viewerBlocks) {
            Text(
                text = stringResource(R.string.blocked),
                style = typography.caption,
                color = colors.reaction,
                modifier = Modifier
                    .background(
                        colors.reaction.copy(alpha = 0.10f),
                        RoundedCornerShape(AppShapes.tagRadius)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun ProfileStateDispatch(
    actor: pub.hackers.android.domain.model.Actor?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    content: @Composable (pub.hackers.android.domain.model.Actor) -> Unit,
) {
    when {
        isLoading && actor == null -> {
            FullScreenLoading()
        }

        error != null && actor == null -> {
            ErrorMessage(
                message = error,
                onRetry = onRetry,
            )
        }

        actor != null -> {
            content(actor)
        }
    }
}
