package pub.hackers.android.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric for htmlToPlainText, which goes through android.text.Html.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PostShareTextTest {

    private val url = "https://hackers.pub/@alice/0193"

    @Test
    fun `article link share puts the title in corner brackets above the url`() {
        assertEquals("『Kotlin Coroutines 101』\n$url", buildLinkShareText("Kotlin Coroutines 101", url))
    }

    @Test
    fun `note link share is just the url`() {
        assertEquals(url, buildLinkShareText(null, url))
    }

    @Test
    fun `short note selection is quoted whole without markers`() {
        assertEquals(
            "\"a scope owns its children\"\n$url",
            buildSelectionShareText(null, "  a scope owns its children\n", url),
        )
    }

    @Test
    fun `short article selection is quoted under the title`() {
        assertEquals(
            "『Kotlin Coroutines 101』\n\"a scope owns its children\"\n$url",
            buildSelectionShareText("Kotlin Coroutines 101", "a scope owns its children", url),
        )
    }

    @Test
    fun `inline image placeholders are dropped from the selection`() {
        assertEquals("\"a scope owns\"\n$url", buildSelectionShareText(null, "a scope ￼owns", url))
    }

    @Test
    fun `long selection keeps its first and last words around a cut`() {
        val words = (1..120).map { "word$it" }
        val share = buildSelectionShareText(null, words.joinToString(" "), url)
        val quote = share.lines()[0]

        assertTrue(quote, quote.startsWith("\"word1 word2 "))
        assertTrue(quote, quote.endsWith(" word119 word120\""))
        assertEquals(1, Regex("""\(\.\.\.\)""").findAll(quote).count())
        assertFitsOnX(share)
    }

    @Test
    fun `cut is balanced between start and end`() {
        val words = (1..120).map { "w$it" }
        val quote = buildSelectionShareText(null, words.joinToString(" "), url).lines()[0]
        val (head, tail) = quote.removeSurrounding("\"").split(" (...) ")

        val difference = head.split(" ").size - tail.split(" ").size
        assertTrue("head=$head tail=$tail", difference in 0..1)
    }

    @Test
    fun `korean counts double toward the limit and is cut on spaces`() {
        val sentence = "구조화된 동시성은 스코프가 자식 코루틴을 소유한다는 뜻입니다."
        val share = buildSelectionShareText(null, List(12) { sentence }.joinToString(" "), url)

        assertTrue(share, share.contains(" (...) "))
        assertTrue(share, share.lines()[0].startsWith("\"구조화된 동시성은"))
        assertFitsOnX(share)
    }

    @Test
    fun `text without spaces is cut by characters`() {
        val japanese = "構造化された並行性とはスコープが子コルーチンを所有することです。".repeat(10)
        val share = buildSelectionShareText(null, japanese, url)

        assertTrue(share, share.contains(" (...) "))
        assertTrue(share, share.lines()[0].startsWith("\"構造化"))
        assertFitsOnX(share)
    }

    @Test
    fun `a long title shrinks the room left for the quote`() {
        val title = "A".repeat(100)
        val selection = (1..80).joinToString(" ") { "word$it" }

        assertFitsOnX(buildSelectionShareText(title, selection, url))
    }

    @Test
    fun `x weighted length counts latin once and cjk, hangul and emoji twice`() {
        assertEquals(3, xWeightedLength("abc"))
        assertEquals(4, xWeightedLength("한글"))
        assertEquals(4, xWeightedLength("漢字"))
        assertEquals(2, xWeightedLength("😀"))
        assertEquals(4, xWeightedLength("『』"))
    }

    @Test
    fun `htmlToPlainText strips markup and decodes entities`() {
        assertEquals("A & B", htmlToPlainText("<p>A &amp; <strong>B</strong></p>"))
    }

    private fun assertFitsOnX(share: String) {
        // X counts any URL as 23 characters.
        val weighted = xWeightedLength(share.replace(url, "")) + 23
        assertTrue("weighted length $weighted > 280:\n$share", weighted <= X_MAX_WEIGHTED_LENGTH)
    }
}
