package pub.hackers.android.ui.screens.postdetail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import pub.hackers.android.ui.theme.AppTypographyDefaults
import pub.hackers.android.ui.theme.LightAppColors
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostDetailContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders replying to handle when replyTarget is not null`() {
        val replyTarget = makePost(
            id = "reply-target",
            actorHandle = "replied-to@hackers.pub",
            content = "reply target body"
        )
        val post = makePost(
            id = "main",
            actorHandle = "author@hackers.pub",
            content = "main body",
            replyTarget = replyTarget
        )

        setContent(post)

        // The "replying to @handle" indicator is unique to PostDetailContent's reply target section
        composeRule.onNodeWithText("Replying to @replied-to@hackers.pub").assertIsDisplayed()
    }

    @Test
    fun `PostDetailStateDispatch shows loading when isLoading`() {
        composeRule.setContent {
            TestTheme {
                PostDetailStateDispatch(
                    post = null,
                    isLoading = true,
                    error = null,
                    onRetry = {},
                ) { Text("content-sentinel") }
            }
        }
        composeRule.onAllNodesWithText("content-sentinel").assertCountEquals(0)
    }

    @Test
    fun `PostDetailStateDispatch shows error when error is not null`() {
        composeRule.setContent {
            TestTheme {
                PostDetailStateDispatch(
                    post = null,
                    isLoading = false,
                    error = "Boom",
                    onRetry = {},
                ) { Text("content-sentinel") }
            }
        }
        composeRule.onNodeWithText("Boom").assertIsDisplayed()
        composeRule.onAllNodesWithText("content-sentinel").assertCountEquals(0)
    }

    @Test
    fun `PostDetailStateDispatch onRetry fires when Retry clicked`() {
        var retryCount = 0
        composeRule.setContent {
            TestTheme {
                PostDetailStateDispatch(
                    post = null,
                    isLoading = false,
                    error = "err",
                    onRetry = { retryCount++ },
                ) { Text("content-sentinel") }
            }
        }
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun `PostDetailStateDispatch renders content with post when post is not null`() {
        val post = makePost(
            id = "p1",
            actorHandle = "a@hackers.pub",
            content = "main body here",
        )
        composeRule.setContent {
            TestTheme {
                PostDetailStateDispatch(
                    post = post,
                    isLoading = false,
                    error = null,
                    onRetry = {},
                ) { resolvedPost -> Text("resolved-id-${resolvedPost.id}") }
            }
        }
        composeRule.onNodeWithText("resolved-id-p1").assertIsDisplayed()
    }

    @Test
    fun `renders quoted post body when quotedPost is not null`() {
        val quoted = makePost(
            id = "quoted",
            actorHandle = "quoted-author@hackers.pub",
            content = "quoted body text"
        )
        val post = makePost(
            id = "main",
            actorHandle = "author@hackers.pub",
            content = "main body",
            quotedPost = quoted
        )

        setContent(post)

        composeRule.onNodeWithText("quoted body text").assertIsDisplayed()
    }

    private fun setContent(post: Post) {
        val repliesFlow = flowOf(PagingData.empty<Post>())
        composeRule.setContent {
            TestTheme {
                val replies = repliesFlow.collectAsLazyPagingItems()
                PostDetailContent(
                    post = post,
                    reactionGroups = emptyList(),
                    toc = emptyList(),
                    replies = replies,
                    onProfileClick = {},
                    onPostClick = {},
                    onShareClick = {},
                    onReplyClick = {},
                    onReactionClick = {},
                    onReactionPickerClick = {},
                    onBookmarkClick = {},
                    onQuoteClick = {},
                    onSharesClick = {},
                    onQuotesClick = {},
                    onReactionsClick = {},
                    onExternalShare = {},
                )
            }
        }
    }

    private fun makePost(
        id: String,
        actorHandle: String,
        content: String,
        replyTarget: Post? = null,
        quotedPost: Post? = null,
    ) = Post(
        id = id,
        typename = "Note",
        name = null,
        published = Instant.parse("2025-01-01T00:00:00Z"),
        summary = null,
        content = "<p>$content</p>",
        excerpt = content,
        url = null,
        viewerHasShared = false,
        actor = Actor(
            id = "actor-$id",
            name = null,
            handle = actorHandle,
            avatarUrl = "https://example.com/avatar.png"
        ),
        media = emptyList(),
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = emptyList(),
        replyTarget = replyTarget,
        quotedPost = quotedPost,
    )
}

@Composable
private fun TestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppColors provides LightAppColors,
        LocalAppTypography provides AppTypographyDefaults,
    ) {
        MaterialTheme(content = content)
    }
}
