package pub.hackers.android.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pub.hackers.android.R
import androidx.compose.runtime.CompositionLocalProvider
import pub.hackers.android.ui.components.BottomNavBar
import pub.hackers.android.ui.components.BottomNavItem
import pub.hackers.android.ui.components.LocalFontScale
import pub.hackers.android.ui.components.ProvideInAppBrowserUriHandler
import pub.hackers.android.ui.screens.auth.SignInScreen
import pub.hackers.android.ui.screens.bookmarks.BookmarksScreen
import pub.hackers.android.ui.screens.compose.ComposeArticleScreen
import pub.hackers.android.ui.screens.compose.ComposeScreen
import pub.hackers.android.ui.screens.drafts.DraftsScreen
import pub.hackers.android.ui.screens.explore.ExploreScreen
import pub.hackers.android.ui.screens.news.NewsScreen
import pub.hackers.android.ui.screens.notifications.NotificationsScreen
import pub.hackers.android.ui.screens.postdetail.PostByUrlResolverScreen
import pub.hackers.android.ui.screens.postdetail.PostDetailScreen
import pub.hackers.android.ui.screens.editprofile.EditProfileScreen
import pub.hackers.android.ui.screens.profile.ProfileScreen
import pub.hackers.android.ui.screens.recommendedactors.RecommendedActorsScreen
import pub.hackers.android.ui.screens.search.SearchScreen
import pub.hackers.android.ui.screens.licenses.LicensesScreen
import pub.hackers.android.ui.screens.settings.SettingsScreen
import pub.hackers.android.ui.screens.timeline.TimelineScreen
import pub.hackers.android.ui.screens.webview.WebViewScreen

private const val PROFILE_REFRESH_KEY = "profile_refresh"
private const val POST_DETAIL_REFRESH_KEY = "post_detail_refresh"

sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val requiresAuth: Boolean = false
) {
    data object Timeline : Screen(
        "timeline",
        R.string.nav_timeline,
        Icons.Filled.Home,
        Icons.Outlined.Home,
        requiresAuth = true
    )
    data object News : Screen(
        "news",
        R.string.nav_news,
        Icons.Filled.Home,
        Icons.Outlined.Home,
        requiresAuth = true
    )
    data object Notifications : Screen(
        "notifications",
        R.string.nav_notifications,
        Icons.Filled.Notifications,
        Icons.Outlined.Notifications,
        requiresAuth = true
    )
    data object Explore : Screen(
        "explore",
        R.string.nav_explore,
        Icons.Filled.Explore,
        Icons.Outlined.Explore
    )
    data object Search : Screen(
        "search?initialQuery={initialQuery}",
        R.string.nav_search,
        Icons.Filled.Search,
        Icons.Outlined.Search
    ) {
        val baseRoute = "search"
        fun createRoute(initialQuery: String? = null): String {
            return if (initialQuery != null) {
                val encoded = android.net.Uri.encode(initialQuery)
                "search?initialQuery=$encoded"
            } else "search"
        }
    }
    data object Settings : Screen(
        "settings",
        R.string.nav_settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

private val TOP_LEVEL_ROUTES = listOf(
    Screen.Timeline.route,
    Screen.News.route,
    Screen.Explore.route,
    Screen.Notifications.route,
    Screen.Search.route,
    Screen.Settings.route,
)

sealed class DetailScreen(val route: String) {
    data object SignIn : DetailScreen("signin?token={token}&code={code}") {
        fun createRoute(token: String? = null, code: String? = null): String {
            val params = mutableListOf<String>()
            if (token != null) params.add("token=$token")
            if (code != null) params.add("code=$code")
            return if (params.isEmpty()) "signin" else "signin?${params.joinToString("&")}"
        }
    }
    data object Compose : DetailScreen("compose?replyTo={replyTo}&quoteOf={quoteOf}&prefill={prefill}&editPost={editPost}") {
        fun createRoute(
            replyTo: String? = null,
            quoteOf: String? = null,
            prefill: String? = null,
            editPost: String? = null,
        ): String {
            val params = mutableListOf<String>()
            if (replyTo != null) params.add("replyTo=${android.net.Uri.encode(replyTo)}")
            if (quoteOf != null) params.add("quoteOf=${android.net.Uri.encode(quoteOf)}")
            if (prefill != null) params.add("prefill=${android.net.Uri.encode(prefill)}")
            if (editPost != null) params.add("editPost=${android.net.Uri.encode(editPost)}")
            return if (params.isEmpty()) "compose" else "compose?${params.joinToString("&")}"
        }
    }
    data object PostDetail : DetailScreen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    data object Profile : DetailScreen("profile/{handle}") {
        fun createRoute(handle: String) = "profile/$handle"
    }
    data object PostByUrl : DetailScreen("post-by-url?url={url}") {
        fun createRoute(url: String): String {
            val encoded = android.net.Uri.encode(url)
            return "post-by-url?url=$encoded"
        }
    }
    data object RecommendedActors : DetailScreen("recommended-actors")
    data object ComposeArticle : DetailScreen("compose-article?draftId={draftId}") {
        fun createRoute(draftId: String? = null): String {
            return if (draftId != null) "compose-article?draftId=$draftId" else "compose-article"
        }
    }
    data object Drafts : DetailScreen("drafts")
    data object Bookmarks : DetailScreen("bookmarks")
    data object EditProfile : DetailScreen("edit-profile")
    data object WebView : DetailScreen("webview?url={url}") {
        fun createRoute(url: String): String {
            val encoded = android.net.Uri.encode(url)
            return "webview?url=$encoded"
        }
    }
    data object Licenses : DetailScreen("licenses")
}

@Composable
fun HackersPubApp(
    deepLinkData: pub.hackers.android.DeepLinkData? = null,
    navigationIntent: pub.hackers.android.NavigationIntent? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onNavigationIntentConsumed: () -> Unit = {},
    viewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedInState by viewModel.isLoggedIn.collectAsState(initial = null as Boolean?)

    val fontSizePercent by viewModel.preferencesManager.fontSizePercent.collectAsState(initial = 100)
    val showLanguageFilter by viewModel.preferencesManager.showLanguageFilter.collectAsState(initial = false)
    val hasUnread by viewModel.hasUnread.collectAsState()
    var selectedHomeFeed by remember { mutableStateOf(HomeFeed.TIMELINE) }

    // Wait for auth state to resolve from DataStore before rendering navigation.
    // This prevents NavHost graph recreation when isLoggedIn transitions from the
    // initial value to the actual value, which would reset the back stack.
    if (isLoggedInState == null) return
    val isLoggedIn = isLoggedInState!!

    // Pin startDestination so it never changes after first resolution.
    // Login/logout transitions are handled imperatively with popUpTo(0).
    val startDestination = remember {
        if (isLoggedIn) Screen.Timeline.route else Screen.Explore.route
    }

    // Handle deep link for verification
    LaunchedEffect(deepLinkData) {
        deepLinkData?.let {
            navController.navigate("signin?token=${it.token}&code=${it.code}")
            onDeepLinkConsumed()
        }
    }

    // Handle navigation intent from system notifications or deep links.
    // Uses the same options as bottom-nav onItemSelected so that back-stack
    // save/restore markers line up; otherwise Timeline (=startDestination)
    // gets stuck in a no-op state on the first bottom-nav tap after arriving
    // here from a system notification.
    LaunchedEffect(navigationIntent) {
        navigationIntent?.let {
            navController.navigate(it.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onNavigationIntentConsumed()
        }
    }

    // Enqueue background polling fallback and register FCM token on login;
    // unregister on the true→false transition so cold starts while logged
    // out don't fire a guaranteed-to-fail unauthenticated mutation.
    var prevLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.enqueueNotificationPolling()
            viewModel.registerFcmToken()
        } else if (prevLoggedIn == true) {
            viewModel.unregisterFcmToken()
        }
        prevLoggedIn = isLoggedIn
    }

    ProvideInAppBrowserUriHandler(
        preferencesManager = viewModel.preferencesManager,
        onInternalNavigate = { route ->
            navController.navigate(route) { launchSingleTop = true }
        }
    ) {
    CompositionLocalProvider(LocalFontScale provides (fontSizePercent / 100f)) {

    val bottomNavItems = if (isLoggedIn) {
        listOf(
            BottomNavItem(
                route = selectedHomeFeed.route,
                label = stringResource(R.string.nav_timeline),
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
            ),
            BottomNavItem(
                route = Screen.Explore.route,
                label = stringResource(R.string.nav_explore),
                icon = Icons.Outlined.Explore,
                selectedIcon = Icons.Filled.Explore,
            ),
            BottomNavItem(
                route = Screen.Notifications.route,
                label = stringResource(R.string.nav_notifications),
                icon = Icons.Outlined.Notifications,
                selectedIcon = Icons.Filled.Notifications,
                hasNotificationDot = hasUnread,
            ),
            BottomNavItem(
                route = Screen.Search.baseRoute,
                label = stringResource(R.string.nav_search),
                icon = Icons.Outlined.Search,
                selectedIcon = Icons.Filled.Search,
            ),
        )
    } else {
        listOf(
            BottomNavItem(
                route = Screen.Explore.route,
                label = stringResource(R.string.nav_explore),
                icon = Icons.Outlined.Explore,
                selectedIcon = Icons.Filled.Explore,
            ),
            BottomNavItem(
                route = Screen.Search.baseRoute,
                label = stringResource(R.string.nav_search),
                icon = Icons.Outlined.Search,
                selectedIcon = Icons.Filled.Search,
            ),
            BottomNavItem(
                route = Screen.Settings.route,
                label = stringResource(R.string.nav_settings),
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
            ),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentBaseRoute = currentRoute?.substringBefore('?')
    val currentHomeFeed = HomeFeed.fromRoute(currentBaseRoute)
    LaunchedEffect(currentHomeFeed) {
        if (currentHomeFeed != null) {
            selectedHomeFeed = currentHomeFeed
        }
    }
    val selectedBottomRoute = currentHomeFeed?.route ?: (currentBaseRoute ?: "")
    val showBottomBar = bottomNavItems.any { it.route == currentBaseRoute } ||
        (isLoggedIn && currentHomeFeed != null)

    // Every account change lands on a fresh root. popUpTo(0) alone isn't enough:
    // bottom-nav tabs navigate with saveState/restoreState, so a tab that isn't
    // on the back stack can still hold a saved back stack (and its ViewModels)
    // that the next tab tap would restore — with the previous account's data.
    fun resetToRoot(route: String) {
        navController.navigate(route) {
            popUpTo(0) { inclusive = true }
        }
        TOP_LEVEL_ROUTES
            .filter { it != route }
            .forEach { navController.clearBackStack(it) }
    }

    fun navigateHomeFeed(feed: HomeFeed) {
        selectedHomeFeed = feed
        navController.navigate(feed.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = bottomNavItems,
                    selectedRoute = selectedBottomRoute,
                    onItemSelected = { item ->
                        val isHomeItem = HomeFeed.fromRoute(item.route) != null
                        val isCurrentHome = currentHomeFeed != null
                        if (item.route == (currentBaseRoute ?: "")) {
                            // Re-tap on current tab: signal scroll-to-top / refresh
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("tabRetapped", System.currentTimeMillis())
                        } else if (isHomeItem && isCurrentHome) {
                            navigateHomeFeed(selectedHomeFeed)
                        } else {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Timeline.route) { backStackEntry ->
                val tabRetapped by backStackEntry.savedStateHandle
                    .getStateFlow<Long>("tabRetapped", 0L)
                    .collectAsState()
                TimelineScreen(
                    tabRetapped = tabRetapped,
                    showLanguageFilter = showLanguageFilter,
                    selectedHomeFeed = selectedHomeFeed,
                    onHomeFeedSelected = ::navigateHomeFeed,
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onComposeClick = { replyTo ->
                        navController.navigate(DetailScreen.Compose.createRoute(replyTo))
                    },
                    onQuoteClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(quoteOf = postId))
                    },
                    onEditClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(editPost = postId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onRecommendedActorsClick = {
                        navController.navigate(DetailScreen.RecommendedActors.route)
                    },
                    onComposeArticleClick = {
                        navController.navigate(DetailScreen.ComposeArticle.createRoute())
                    },
                    onComposeArticleLongClick = {
                        navController.navigate(DetailScreen.Drafts.route)
                    }
                )
            }

            composable(Screen.News.route) {
                NewsScreen(
                    selectedHomeFeed = selectedHomeFeed,
                    onHomeFeedSelected = ::navigateHomeFeed,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    }
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    showLanguageFilter = showLanguageFilter,
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onReplyClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(postId))
                    },
                    onQuoteClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(quoteOf = postId))
                    },
                    onEditClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(editPost = postId))
                    },
                    onSignInClick = {
                        navController.navigate(DetailScreen.SignIn.createRoute())
                    },
                    isLoggedIn = isLoggedIn
                )
            }

            composable(
                route = Screen.Search.route,
                arguments = listOf(
                    navArgument("initialQuery") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialQuery = backStackEntry.arguments?.getString("initialQuery")
                SearchScreen(
                    initialQuery = initialQuery,
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onReplyClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(replyTo = postId))
                    },
                    onQuoteClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(quoteOf = postId))
                    },
                    onEditClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(editPost = postId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onSignInClick = {
                        navController.navigate(DetailScreen.SignIn.createRoute())
                    },
                    onSignOutComplete = {
                        resetToRoot(Screen.Explore.route)
                    },
                    onAccountSwitched = {
                        resetToRoot(Screen.Timeline.route)
                    },
                    onAddAccountClick = {
                        navController.navigate(DetailScreen.SignIn.createRoute())
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Timeline.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onDraftsClick = {
                        navController.navigate(DetailScreen.Drafts.route)
                    },
                    onBookmarksClick = {
                        navController.navigate(DetailScreen.Bookmarks.route)
                    },
                    onLicensesClick = {
                        navController.navigate(DetailScreen.Licenses.route)
                    },
                    isLoggedIn = isLoggedIn
                )
            }

            composable(
                route = DetailScreen.SignIn.route,
                arguments = listOf(
                    navArgument("token") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("code") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token")
                val code = backStackEntry.arguments?.getString("code")
                SignInScreen(
                    deepLinkToken = token,
                    deepLinkCode = code,
                    onSignInSuccess = {
                        resetToRoot(Screen.Timeline.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = DetailScreen.Compose.route,
                arguments = listOf(
                    navArgument("replyTo") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("quoteOf") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("prefill") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("editPost") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val replyTo = backStackEntry.arguments?.getString("replyTo")
                val quoteOf = backStackEntry.arguments?.getString("quoteOf")
                val prefill = backStackEntry.arguments?.getString("prefill")
                val editPost = backStackEntry.arguments?.getString("editPost")
                ComposeScreen(
                    replyToId = replyTo,
                    quotedPostId = quoteOf,
                    initialContent = prefill,
                    editPostId = editPost,
                    onPostSuccess = {
                        viewModel.timelineRefreshTrigger.requestRefresh()
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(POST_DETAIL_REFRESH_KEY, true)
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = DetailScreen.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                val postDetailRefreshFlag = backStackEntry.savedStateHandle
                    .getStateFlow(POST_DETAIL_REFRESH_KEY, false)
                    .collectAsState()
                PostDetailScreen(
                    postId = postId,
                    refreshSignal = postDetailRefreshFlag.value,
                    onRefreshConsumed = {
                        backStackEntry.savedStateHandle[POST_DETAIL_REFRESH_KEY] = false
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onReplyClick = { id ->
                        navController.navigate(DetailScreen.Compose.createRoute(replyTo = id))
                    },
                    onQuoteClick = { id ->
                        navController.navigate(DetailScreen.Compose.createRoute(quoteOf = id))
                    },
                    onEditClick = { id ->
                        navController.navigate(DetailScreen.Compose.createRoute(editPost = id))
                    },
                    onPostClick = { id ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(id))
                    },
                    isLoggedIn = isLoggedIn
                )
            }

            composable(
                route = DetailScreen.PostByUrl.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: return@composable
                PostByUrlResolverScreen(
                    url = url,
                    onResolved = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId)) {
                            popUpTo(DetailScreen.PostByUrl.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = DetailScreen.Profile.route,
                arguments = listOf(navArgument("handle") { type = NavType.StringType })
            ) { backStackEntry ->
                val handle = backStackEntry.arguments?.getString("handle") ?: return@composable
                val profileRefreshFlag = backStackEntry.savedStateHandle
                    .getStateFlow(PROFILE_REFRESH_KEY, false)
                    .collectAsState()
                ProfileScreen(
                    handle = handle,
                    refreshSignal = profileRefreshFlag.value,
                    onRefreshConsumed = {
                        backStackEntry.savedStateHandle[PROFILE_REFRESH_KEY] = false
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { profileHandle ->
                        navController.navigate(DetailScreen.Profile.createRoute(profileHandle))
                    },
                    onReplyClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(replyTo = postId))
                    },
                    onQuoteClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(quoteOf = postId))
                    },
                    onEditClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(editPost = postId))
                    },
                    onEditProfileClick = {
                        navController.navigate(DetailScreen.EditProfile.route)
                    }
                )
            }

            composable(DetailScreen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(PROFILE_REFRESH_KEY, true)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = DetailScreen.ComposeArticle.route,
                arguments = listOf(
                    navArgument("draftId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val draftId = backStackEntry.arguments?.getString("draftId")
                ComposeArticleScreen(
                    draftId = draftId,
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    onPublishSuccess = { articleId ->
                        navController.popBackStack()
                        navController.navigate(DetailScreen.PostDetail.createRoute(articleId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(DetailScreen.Drafts.route) {
                DraftsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onDraftClick = { draftId ->
                        navController.navigate(DetailScreen.ComposeArticle.createRoute(draftId))
                    }
                )
            }

            composable(DetailScreen.Bookmarks.route) {
                BookmarksScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onReplyClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(replyTo = postId))
                    },
                    onQuoteClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(quoteOf = postId))
                    },
                    onEditClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(editPost = postId))
                    },
                )
            }

            composable(DetailScreen.RecommendedActors.route) {
                RecommendedActorsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    }
                )
            }

            composable(
                route = DetailScreen.WebView.route,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: return@composable
                WebViewScreen(
                    url = url,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(DetailScreen.Licenses.route) {
                LicensesScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
    } // CompositionLocalProvider
    } // ProvideInAppBrowserUriHandler
}
