package pub.hackers.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pub.hackers.android.ui.theme.AppTypographyDefaults
import pub.hackers.android.ui.theme.LightAppColors
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

/**
 * Selection and taps share the same text. A long press belongs to text selection
 * and must not count as a tap, while plain taps must keep reaching links and the
 * tap-through to the post.
 *
 * Robolectric can't observe the selection itself (the text toolbar is never
 * requested, even for a bare SelectionContainer), so these cover the tap side.
 * Runs at API 27: on 28+ a long press shows the platform Magnifier, whose
 * dismissal crashes under Robolectric (null Surface in InternalPopupWindow).
 * Compose skips the platform magnifier below 28, and minSdk is 26.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HtmlContentSelectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `long press on post text does not open the post`() {
        var postTaps = 0
        setContent {
            HtmlContent(
                html = "<p>Selectable post body text</p>",
                modifier = Modifier.testTag(CONTENT),
                onTextClick = { postTaps++ },
            )
        }

        composeRule.onNodeWithTag(CONTENT).performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertEquals(0, postTaps)
    }

    @Test
    fun `long press on truncated preview text does not open the post`() {
        var postTaps = 0
        setContent {
            HtmlContent(
                html = "<p>Selectable preview text</p>",
                maxLines = 3,
                modifier = Modifier.testTag(CONTENT),
                onTextClick = { postTaps++ },
            )
        }

        composeRule.onNodeWithTag(CONTENT).performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertEquals(0, postTaps)
    }

    @Test
    fun `tap on post text still opens the post`() {
        var postTaps = 0
        setContent {
            HtmlContent(
                html = "<p>Selectable post body text</p>",
                modifier = Modifier.testTag(CONTENT),
                onTextClick = { postTaps++ },
            )
        }

        composeRule.onNodeWithTag(CONTENT).performClick()
        composeRule.waitForIdle()

        assertEquals(1, postTaps)
    }

    @Test
    fun `tap on a link still follows the link`() {
        val opened = mutableListOf<String>()
        setContent {
            HtmlContent(
                html = """<p><a href="https://hackers.pub/">hackers.pub</a></p>""",
                modifier = Modifier.testTag(CONTENT),
                onLinkClick = { opened += it },
            )
        }

        composeRule.onNodeWithTag(CONTENT).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("https://hackers.pub/"), opened)
    }

    @Test
    fun `tap on a selectable card title still reaches the card`() {
        var cardTaps = 0
        setContent {
            Column(modifier = Modifier.clickable { cardTaps++ }) {
                SelectionContainer {
                    Text("Article title")
                }
            }
        }

        composeRule.onNodeWithText("Article title").performClick()
        composeRule.waitForIdle()

        assertEquals(1, cardTaps)
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalAppColors provides LightAppColors,
                LocalAppTypography provides AppTypographyDefaults,
            ) {
                MaterialTheme(content = content)
            }
        }
    }

    private companion object {
        const val CONTENT = "content"
    }
}
