package pub.hackers.android.ui.screens.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import pub.hackers.android.ui.components.CompactTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pub.hackers.android.R
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.LoadingItem
import pub.hackers.android.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    isLoggedIn: Boolean,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasNextPage && !uiState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(uiState.selectedTab) {
        listState.scrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CompactTopBar(
                title = stringResource(R.string.nav_explore),
                actions = {
                    if (!isLoggedIn) {
                        TextButton(onClick = onSignInClick) {
                            Text(stringResource(R.string.sign_in))
                        }
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
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal
            ) {
                ExploreTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    ExploreTab.LOCAL -> stringResource(R.string.local_timeline)
                                    ExploreTab.GLOBAL -> stringResource(R.string.global_timeline)
                                }
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.posts.isEmpty() -> {
                        FullScreenLoading()
                    }
                    uiState.error != null && uiState.posts.isEmpty() -> {
                        ErrorMessage(
                            message = uiState.error ?: stringResource(R.string.error_generic),
                            onRetry = { viewModel.refresh() }
                        )
                    }
                    uiState.posts.isEmpty() -> {
                        ErrorMessage(
                            message = stringResource(R.string.no_posts)
                        )
                    }
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() }
                        ) {
                            LazyColumn(
                                state = listState
                            ) {
                                items(
                                    items = uiState.posts,
                                    key = { it.id }
                                ) { post ->
                                    PostCard(
                                        post = post,
                                        onClick = { onPostClick(post.sharedPost?.id ?: post.id) },
                                        onProfileClick = onProfileClick,
                                        onShareClick = if (isLoggedIn) {
                                            {
                                                if (post.viewerHasShared) {
                                                    viewModel.unsharePost(post.id)
                                                } else {
                                                    viewModel.sharePost(post.id)
                                                }
                                            }
                                        } else null
                                    )
                                    HorizontalDivider(thickness = 0.5.dp)
                                }

                                if (uiState.isLoadingMore) {
                                    item {
                                        LoadingItem()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
