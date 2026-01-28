package pub.hackers.android.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pub.hackers.android.R
import pub.hackers.android.ui.screens.auth.SignInScreen
import pub.hackers.android.ui.screens.compose.ComposeScreen
import pub.hackers.android.ui.screens.explore.ExploreScreen
import pub.hackers.android.ui.screens.notifications.NotificationsScreen
import pub.hackers.android.ui.screens.postdetail.PostDetailScreen
import pub.hackers.android.ui.screens.profile.ProfileScreen
import pub.hackers.android.ui.screens.search.SearchScreen
import pub.hackers.android.ui.screens.settings.SettingsScreen
import pub.hackers.android.ui.screens.timeline.TimelineScreen

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
        "search",
        R.string.nav_search,
        Icons.Filled.Search,
        Icons.Outlined.Search
    )
    data object Settings : Screen(
        "settings",
        R.string.nav_settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

sealed class DetailScreen(val route: String) {
    data object SignIn : DetailScreen("signin")
    data object Compose : DetailScreen("compose?replyTo={replyTo}") {
        fun createRoute(replyTo: String? = null) =
            if (replyTo != null) "compose?replyTo=$replyTo" else "compose"
    }
    data object PostDetail : DetailScreen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    data object Profile : DetailScreen("profile/{handle}") {
        fun createRoute(handle: String) = "profile/$handle"
    }
}

@Composable
fun HackersPubApp(
    viewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)

    val bottomNavItems = if (isLoggedIn) {
        listOf(Screen.Timeline, Screen.Notifications, Screen.Explore, Screen.Search, Screen.Settings)
    } else {
        listOf(Screen.Explore, Screen.Search, Screen.Settings)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(56.dp),
                    windowInsets = WindowInsets(0)
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = stringResource(screen.titleResId)
                                )
                            },
                            label = null,
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Timeline.route else Screen.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onComposeClick = { replyTo ->
                        navController.navigate(DetailScreen.Compose.createRoute(replyTo))
                    }
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
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onReplyClick = { postId ->
                        navController.navigate(DetailScreen.Compose.createRoute(postId))
                    },
                    onSignInClick = {
                        navController.navigate(DetailScreen.SignIn.route)
                    },
                    isLoggedIn = isLoggedIn
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onSignInClick = {
                        navController.navigate(DetailScreen.SignIn.route)
                    },
                    onSignOutComplete = {
                        navController.navigate(Screen.Explore.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    isLoggedIn = isLoggedIn
                )
            }

            composable(DetailScreen.SignIn.route) {
                SignInScreen(
                    onSignInSuccess = {
                        navController.navigate(Screen.Timeline.route) {
                            popUpTo(0) { inclusive = true }
                        }
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
                    }
                )
            ) { backStackEntry ->
                val replyTo = backStackEntry.arguments?.getString("replyTo")
                ComposeScreen(
                    replyToId = replyTo,
                    onPostSuccess = {
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
                PostDetailScreen(
                    postId = postId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProfileClick = { handle ->
                        navController.navigate(DetailScreen.Profile.createRoute(handle))
                    },
                    onReplyClick = { id ->
                        navController.navigate(DetailScreen.Compose.createRoute(id))
                    },
                    onPostClick = { id ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(id))
                    }
                )
            }

            composable(
                route = DetailScreen.Profile.route,
                arguments = listOf(navArgument("handle") { type = NavType.StringType })
            ) { backStackEntry ->
                val handle = backStackEntry.arguments?.getString("handle") ?: return@composable
                ProfileScreen(
                    handle = handle,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPostClick = { postId ->
                        navController.navigate(DetailScreen.PostDetail.createRoute(postId))
                    }
                )
            }
        }
    }
}
