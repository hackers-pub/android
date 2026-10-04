package pub.hackers.android.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.Poll
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.RichDisplayName
import pub.hackers.android.ui.share.sharePlainText
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun SearchScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit = {},
    onQuoteClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {},
    initialQuery: String? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    LaunchedEffect(initialQuery) {
        if (initialQuery != null && !uiState.hasSearched) {
            viewModel.updateQuery(initialQuery)
            viewModel.search()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(title = stringResource(R.string.nav_search))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Custom styled search bar
            BasicTextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                singleLine = true,
                textStyle = typography.bodyLarge.copy(color = colors.textBody),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.search()
                        keyboardController?.hide()
                    }
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                color = colors.surface,
                                shape = RoundedCornerShape(AppShapes.searchBarRadius)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = typography.bodyLarge,
                                    color = colors.textSecondary
                                )
                            }
                            innerTextField()
                        }

                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            SearchModeChips(
                selected = uiState.mode,
                onSelect = { viewModel.setMode(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        FullScreenLoading()
                    }
                    uiState.error != null -> {
                        ErrorMessage(
                            message = uiState.error ?: stringResource(R.string.error_generic),
                            onRetry = { viewModel.search() }
                        )
                    }
                    uiState.mode == SearchMode.ALL -> {
                        val allActors = uiState.actors.take(5)
                        val hasActors = allActors.isNotEmpty()
                        val hasPosts = uiState.posts.isNotEmpty()
                        when {
                            uiState.hasSearched && !hasActors && !hasPosts -> {
                                ErrorMessage(message = stringResource(R.string.no_results))
                            }
                            hasActors || hasPosts -> {
                                LazyColumn {
                                    if (hasActors) {
                                        item(key = "header-actors") {
                                            SearchSectionHeader(stringResource(R.string.search_people))
                                        }
                                        items(
                                            items = allActors,
                                            key = { "actor-${it.id}" }
                                        ) { actor ->
                                            SearchActorRow(
                                                actor = actor,
                                                onClick = { onProfileClick(actor.handle) }
                                            )
                                            HorizontalDivider(
                                                color = colors.divider,
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                    if (hasPosts) {
                                        item(key = "header-posts") {
                                            SearchSectionHeader(stringResource(R.string.search_posts))
                                        }
                                        items(
                                            items = uiState.posts,
                                            key = { "post-${it.id}" }
                                        ) { post ->
                                            SearchPostItem(
                                                post = post,
                                                onPostClick = onPostClick,
                                                onProfileClick = onProfileClick,
                                                onReplyClick = onReplyClick,
                                                onQuoteClick = onQuoteClick,
                                                onEditClick = onEditClick,
                                                onExternalShare = { context.sharePlainText(it) },
                                                onVotePoll = viewModel::voteOnPoll
                                            )
                                            HorizontalDivider(
                                                color = colors.divider,
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> SearchHint()
                        }
                    }
                    uiState.mode == SearchMode.PEOPLE -> {
                        when {
                            uiState.hasSearched && uiState.actors.isEmpty() -> {
                                ErrorMessage(message = stringResource(R.string.no_results))
                            }
                            uiState.actors.isNotEmpty() -> {
                                LazyColumn {
                                    items(
                                        items = uiState.actors,
                                        key = { it.id }
                                    ) { actor ->
                                        SearchActorRow(
                                            actor = actor,
                                            onClick = { onProfileClick(actor.handle) }
                                        )
                                        HorizontalDivider(
                                            color = colors.divider,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                            else -> SearchHint()
                        }
                    }
                    else -> {
                        val postList = if (uiState.mode == SearchMode.TAGS) {
                            uiState.taggedPosts
                        } else {
                            uiState.posts
                        }
                        when {
                            uiState.hasSearched && postList.isEmpty() -> {
                                ErrorMessage(message = stringResource(R.string.no_results))
                            }
                            postList.isNotEmpty() -> {
                                LazyColumn {
                                    items(
                                        items = postList,
                                        key = { it.id }
                                    ) { post ->
                                        SearchPostItem(
                                            post = post,
                                            onPostClick = onPostClick,
                                            onProfileClick = onProfileClick,
                                            onReplyClick = onReplyClick,
                                            onQuoteClick = onQuoteClick,
                                            onEditClick = onEditClick,
                                            onExternalShare = { context.sharePlainText(it) },
                                            onVotePoll = viewModel::voteOnPoll
                                        )
                                        HorizontalDivider(
                                            color = colors.divider,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                            else -> SearchHint()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchModeChips(
    selected: SearchMode,
    onSelect: (SearchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeChip(
            label = stringResource(R.string.search_all),
            isSelected = selected == SearchMode.ALL,
            onClick = { onSelect(SearchMode.ALL) }
        )
        ModeChip(
            label = stringResource(R.string.search_people),
            isSelected = selected == SearchMode.PEOPLE,
            onClick = { onSelect(SearchMode.PEOPLE) }
        )
        ModeChip(
            label = stringResource(R.string.search_posts),
            isSelected = selected == SearchMode.POSTS,
            onClick = { onSelect(SearchMode.POSTS) }
        )
        ModeChip(
            label = stringResource(R.string.search_tags),
            isSelected = selected == SearchMode.TAGS,
            onClick = { onSelect(SearchMode.TAGS) }
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.background,
            labelColor = colors.textBody,
            selectedContainerColor = colors.accent,
            selectedLabelColor = colors.background
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = colors.buttonOutline,
            selectedBorderColor = colors.accent
        )
    )
}

@Composable
private fun SearchActorRow(
    actor: Actor,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = actor.avatarUrl,
            contentDescription = actor.name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            RichDisplayName(
                name = actor.name,
                fallback = actor.handle,
                style = typography.bodyLargeSemiBold,
                color = colors.textPrimary
            )
            Text(
                text = actor.handle,
                style = typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SearchHint() {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = stringResource(R.string.search_hint),
            style = typography.bodyLarge,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider(
            color = colors.divider,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SearchPostItem(
    post: pub.hackers.android.domain.model.Post,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onExternalShare: (String) -> Unit,
    onVotePoll: (suspend (questionId: String, optionIndices: List<Int>) -> Result<Poll>)? = null
) {
    PostCard(
        post = post,
        onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
        onProfileClick = onProfileClick,
        onReplyClick = { onReplyClick(post.sharedPost?.id ?: post.id) },
        onQuoteClick = { onQuoteClick(post.sharedPost?.id ?: post.id) },
        onEditClick = { onEditClick(it.id) },
        onReactionClick = null,
        onExternalShare = onExternalShare,
        onQuotedPostClick = onPostClick,
        onVotePoll = onVotePoll
    )
}
