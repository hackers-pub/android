package pub.hackers.android.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lays parsed list HTML out for real and counts lines. The parser's text alone
 * can't catch spacing bugs here: Compose breaks lines at every ParagraphStyle
 * boundary, so a stray "\n" next to one renders as a blank line even though the
 * text looks right. Needs native graphics for real text measurement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HtmlListLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `compact list items render without blank lines between them`() {
        assertEquals(2, lineCount("<ul><li>item1</li><li>item2</li></ul>", HtmlContentStyle.Compact))
    }

    @Test
    fun `list items wrapped in paragraphs render without blank lines between them`() {
        assertEquals(
            2,
            lineCount("<ul><li><p>item1</p></li><li><p>item2</p></li></ul>", HtmlContentStyle.Compact),
        )
    }

    @Test
    fun `prose list items render without blank lines between them`() {
        assertEquals(2, lineCount("<ul><li>item1</li><li>item2</li></ul>", HtmlContentStyle.Prose))
    }

    @Test
    fun `nested list items render without blank lines between them`() {
        assertEquals(
            3,
            lineCount(
                "<ul><li>parent<ul><li>child</li></ul></li><li>next</li></ul>",
                HtmlContentStyle.Compact,
            ),
        )
    }

    // before, blank, item1, item2, blank, after
    @Test
    fun `compact list is separated from surrounding paragraphs by one blank line`() {
        assertEquals(6, lineCount(LIST_BETWEEN_PARAGRAPHS, HtmlContentStyle.Compact))
    }

    @Test
    fun `prose list is separated from surrounding paragraphs by one blank line`() {
        assertEquals(6, lineCount(LIST_BETWEEN_PARAGRAPHS, HtmlContentStyle.Prose))
    }

    private fun lineCount(html: String, style: HtmlContentStyle): Int {
        val text = parseHtmlToAnnotatedString(
            html = html,
            linkColor = Color.Blue,
            hashtagColor = Color.Green,
            mentionBg = Color.Gray,
            codeBg = Color.LightGray,
            contentStyle = style,
        )
        return measure(text)
    }

    private fun measure(text: AnnotatedString): Int {
        var lines = -1
        composeRule.setContent {
            BasicText(text, onTextLayout = { lines = it.lineCount })
        }
        composeRule.waitForIdle()
        return lines
    }

    private companion object {
        const val LIST_BETWEEN_PARAGRAPHS =
            "<p>before</p><ul><li>item1</li><li>item2</li></ul><p>after</p>"
    }
}
