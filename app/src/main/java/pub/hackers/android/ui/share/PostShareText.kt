package pub.hackers.android.ui.share

import android.text.Html
import pub.hackers.android.domain.model.Post

/*
 * Text handed to other apps when a post is shared. Titles go in 『』, quoted post
 * text in straight double quotes, and the post URL always ends the text:
 *
 *   『Title』                     ← articles only
 *   "first words (...) last words"
 *   https://hackers.pub/@alice/…
 *
 * Shares are sized to fit a single post on X: a quote too long for that keeps
 * as many words from its start and end as fit and cuts the middle with (...).
 */

private const val CUT_MARKER = "(...)"

// Inline images (custom emoji, mention avatars) occupy this placeholder in
// rendered text, so it ends up in selected text.
private const val OBJECT_REPLACEMENT_CHAR = '￼'
private val WHITESPACE = Regex("""\s+""")

// X's limit, in its weighted length: URLs count as a fixed t.co length, and
// characters outside the Latin/punctuation ranges (CJK, Hangul, emoji) count
// double. See twitter-text's v3 configuration.
internal const val X_MAX_WEIGHTED_LENGTH = 280
private const val X_URL_LENGTH = 23

/** URL shared for a post; falls back to the ActivityPub IRI like the old share button. */
fun Post.shareUrl(): String? = url ?: iri

/** Title shown in shares. Only articles have one worth quoting. */
fun Post.shareTitle(): String? = name?.takeIf { typename == "Article" && it.isNotBlank() }

fun htmlToPlainText(html: String): String =
    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

/** `『title』` + URL for articles, the bare URL for notes. */
fun buildLinkShareText(title: String?, url: String): String =
    listOfNotNull(title?.let(::titleLine), url).joinToString("\n")

/**
 * A selected part of a post, quoted. Quoted whole when the share fits on X;
 * otherwise cut down to its first and last words around (...).
 */
fun buildSelectionShareText(title: String?, selection: String, url: String): String {
    val titleLine = title?.let(::titleLine)
    val excerpt = selection.replace(OBJECT_REPLACEMENT_CHAR.toString(), "").trim()
    // Everything but the quote's inner text: title line, quotes, URL, line breaks.
    val overhead = (titleLine?.let { xWeightedLength(it) + 1 } ?: 0) + 2 + 1 + X_URL_LENGTH
    val quoted = fitExcerpt(excerpt, X_MAX_WEIGHTED_LENGTH - overhead)
    return listOfNotNull(titleLine, quote(quoted), url).joinToString("\n")
}

/**
 * Returns [excerpt] unchanged if it fits [budget]; otherwise keeps a balanced
 * run of leading and trailing words joined by (...). Text without enough
 * spaces to cut on (e.g. Chinese or Japanese) is cut by characters instead.
 */
internal fun fitExcerpt(excerpt: String, budget: Int): String {
    if (xWeightedLength(excerpt) <= budget) return excerpt
    val words = excerpt.split(WHITESPACE).filter { it.isNotEmpty() }
    return cutMiddle(words, separator = " ", budget = budget)
        ?: cutMiddle(excerpt.codePoints().toArray().map { String(Character.toChars(it)) }, "", budget)
        ?: CUT_MARKER
}

/**
 * Takes units alternately from the front and the back while the joined result
 * still fits. Null if not even one unit from each end fits.
 */
private fun cutMiddle(units: List<String>, separator: String, budget: Int): String? {
    if (units.size < 2) return null
    fun render(head: Int, tail: Int) = buildString {
        append(units.subList(0, head).joinToString(separator))
        append(' ').append(CUT_MARKER).append(' ')
        append(units.subList(units.size - tail, units.size).joinToString(separator))
    }
    var head = 0
    var tail = 0
    while (head + tail < units.size) {
        val takeHead = head <= tail
        val candidate = if (takeHead) render(head + 1, tail) else render(head, tail + 1)
        if (xWeightedLength(candidate) > budget) break
        if (takeHead) head++ else tail++
    }
    return if (head > 0 && tail > 0) render(head, tail) else null
}

/** Length as X counts it for the 280 limit (excluding URLs, handled separately). */
internal fun xWeightedLength(text: String): Int {
    var length = 0
    var i = 0
    while (i < text.length) {
        val codePoint = text.codePointAt(i)
        length += if (isSingleWeight(codePoint)) 1 else 2
        i += Character.charCount(codePoint)
    }
    return length
}

private fun isSingleWeight(codePoint: Int) =
    codePoint in 0x0000..0x10FF ||
        codePoint in 0x2000..0x200D ||
        codePoint in 0x2010..0x201F ||
        codePoint in 0x2032..0x2037

private fun titleLine(title: String) = "『$title』"

private fun quote(text: String) = "\"$text\""
