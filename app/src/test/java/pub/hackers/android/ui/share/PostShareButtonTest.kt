package pub.hackers.android.ui.share

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.EngagementStats
import pub.hackers.android.domain.model.Post
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PostShareButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shared = mutableListOf<String>()

    @Test
    fun `note share button shares the bare link`() {
        setContent(post(typename = "Note"))

        composeRule.onNodeWithContentDescription("Share link").performClick()

        assertEquals(listOf(URL), shared)
    }

    @Test
    fun `article share button shares the link with its title`() {
        setContent(post(typename = "Article", name = "Kotlin Coroutines 101"))

        composeRule.onNodeWithContentDescription("Share link").performClick()

        assertEquals(listOf("『Kotlin Coroutines 101』\n$URL"), shared)
    }

    @Test
    fun `post without a url shows no share button`() {
        setContent(post(typename = "Note", url = null))

        composeRule.onNodeWithContentDescription("Share link").assertDoesNotExist()
    }

    private fun setContent(post: Post) {
        composeRule.setContent {
            MaterialTheme {
                PostShareButton(post = post, onShare = { shared += it }, tint = Color.Gray)
            }
        }
    }

    private fun post(
        typename: String,
        name: String? = null,
        url: String? = URL,
    ) = Post(
        id = "post-1",
        typename = typename,
        name = name,
        published = Instant.parse("2025-01-01T00:00:00Z"),
        summary = null,
        content = "<p>Hello</p>",
        excerpt = "",
        url = url,
        viewerHasShared = false,
        actor = Actor(
            id = "actor-1",
            name = null,
            handle = "@alice@hackers.pub",
            avatarUrl = "https://example.com/avatar.png"
        ),
        media = emptyList(),
        engagementStats = EngagementStats(replies = 0, reactions = 0, shares = 0, quotes = 0),
        mentions = emptyList(),
    )

    private companion object {
        const val URL = "https://hackers.pub/@alice/0193"
    }
}
