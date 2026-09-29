package pub.hackers.android.ui.components

import android.util.LruCache
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import pub.hackers.android.ui.theme.UbuntuMonoFontFamily
import java.net.URI

val LocalFontScale = compositionLocalOf { 1f }

private enum class LinkType {
    MENTION, HASHTAG, REGULAR
}

private enum class SpanKind {
    INVISIBLE, MENTION_NAME, OTHER
}

private data class ListContext(val ordered: Boolean, var itemIndex: Int = 0)

enum class HtmlContentStyle {
    Compact,
    Prose
}

internal sealed class ContentBlock {
    data class Text(val html: String) : ContentBlock()
    data class Code(val codeHtml: String) : ContentBlock()
    data class List(val html: String) : ContentBlock()
    data class Heading(val level: Int, val anchorId: String?, val innerHtml: String) : ContentBlock()
    data class Image(val src: String, val alt: String?) : ContentBlock()
}

@VisibleForTesting
internal data class ParsedListBlock(
    val ordered: Boolean,
    val items: List<ParsedListItem>,
)

@VisibleForTesting
internal data class ParsedListItem(
    val contentHtml: String,
    val children: List<ParsedListBlock>,
)

@VisibleForTesting
internal data class ParsedInlineImage(
    val src: String,
    val alt: String?,
    val width: Int,
    val height: Int,
    val isCustomEmoji: Boolean = false,
)

@VisibleForTesting
internal data class ParsedHtmlContent(
    val text: AnnotatedString,
    val inlineImages: Map<String, ParsedInlineImage>,
) {
    fun isNotEmpty(): Boolean = text.isNotEmpty()
}

private val TAG_REGEX = Regex("""<(/?)(\w+)([^>]*)>""")
private val ATTR_REGEX = Regex("""([\w-]+)=["']([^"']*)["']""")
private val PRE_CODE_REGEX = Regex(
    """<pre[^>]*>\s*<code[^>]*>([\s\S]*?)</code>\s*</pre>""",
    RegexOption.IGNORE_CASE
)
private val HEADING_REGEX = Regex(
    """<h([1-6])([^>]*)>([\s\S]*?)</h\1>""",
    RegexOption.IGNORE_CASE
)
// Matches `<img ...>` (including self-closing `/>`), optionally wrapped in
// a `<p>` that contains nothing else. The outer wrapper is consumed so the
// image extraction doesn't leave stray empty paragraphs in the text stream.
private val IMG_BLOCK_REGEX = Regex(
    """(?:<p[^>]*>\s*)?<img\b([^>]*?)/?>(?:\s*</p>)?""",
    RegexOption.IGNORE_CASE
)
private val EMPTY_PARAGRAPH_REGEX = Regex(
    """<p>\s*(?:<br\s*/?>\s*)*</p>""",
    setOf(RegexOption.IGNORE_CASE)
)
private val LIST_BOUNDARY_BREAK_REGEX = Regex(
    """(</?(?:ul|ol|li)[^>]*>)\s*(?:<br\s*/?>\s*)+""",
    setOf(RegexOption.IGNORE_CASE)
)
private val LIST_BREAK_BEFORE_TAG_REGEX = Regex(
    """(?:<br\s*/?>\s*)+(</?(?:ul|ol|li)[^>]*>)""",
    setOf(RegexOption.IGNORE_CASE)
)
private val BLOCK_TAGS = setOf(
    "p", "div", "blockquote", "ul", "ol", "li", "pre", "h1", "h2", "h3", "h4", "h5", "h6", "hr"
)

// Process-level cache for parsed HTML. Sized to comfortably cover the active
// set across Timeline + Explore + Profile tabs + PostDetail without evicting
// on revisits. Entries are typically 2-5 KB.
private const val HTML_CACHE_MAX_ENTRIES = 256

// Below this html length, parsing runs synchronously during composition; above
// it, parsing is dispatched to Dispatchers.Default via produceState so the
// frame callback can keep rendering smoothly. Tune if placeholder flash is
// visible for intermediate-length posts.
private const val HTML_SYNC_PARSE_THRESHOLD = 500
private const val INLINE_IMAGE_DEFAULT_SIZE = 18
private const val INLINE_IMAGE_MIN_SIZE = 12
private const val INLINE_IMAGE_MAX_SIZE = 48
private const val INLINE_IMAGE_TRAILING_SPACE = 4
private const val CUSTOM_EMOJI_SIZE_EM = 1.2f
private const val INLINE_IMAGE_ALTERNATE_TEXT = "\uFFFC"

private data class HtmlCacheKey(
    val html: String,
    val linkColor: ULong,
    val hashtagColor: ULong,
    val mentionBg: ULong,
    val mentionNameColor: ULong,
    val codeBg: ULong,
    val contentStyle: HtmlContentStyle,
)

private val emptyParsedHtmlContent = ParsedHtmlContent(AnnotatedString(""), emptyMap())

private val htmlCache = LruCache<HtmlCacheKey, ParsedHtmlContent>(HTML_CACHE_MAX_ENTRIES)

private fun parseAndCacheHtml(
    key: HtmlCacheKey,
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    mentionNameColor: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle,
): ParsedHtmlContent {
    val parsed = parseHtmlToContent(
        html = html,
        linkColor = linkColor,
        hashtagColor = hashtagColor,
        mentionBg = mentionBg,
        mentionNameColor = mentionNameColor,
        codeBg = codeBg,
        contentStyle = contentStyle,
    )
    htmlCache.put(key, parsed)
    return parsed
}

/**
 * Returns parsed content for [html], using a process-level LRU cache.
 *
 * - Cache hit or short HTML: parsed synchronously, returned immediately.
 *   No placeholder flash; matches the original behavior.
 * - Long HTML on a cache miss: parsed on [Dispatchers.Default] via
 *   [produceState]. Composition completes immediately with an empty
 *   placeholder; the Text updates on the next frame once parsing finishes.
 *   This prevents the scroll-induced Main-thread stalls previously caused
 *   by [parseHtmlToContent] running inline for every newly-composed
 *   LazyColumn item.
 */
@Composable
private fun rememberParsedHtml(
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    mentionNameColor: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle,
): ParsedHtmlContent {
    val cacheKey = remember(html, linkColor, hashtagColor, mentionBg, mentionNameColor, codeBg, contentStyle) {
        HtmlCacheKey(
            html = html,
            linkColor = linkColor.value,
            hashtagColor = hashtagColor.value,
            mentionBg = mentionBg.value,
            mentionNameColor = mentionNameColor.value,
            codeBg = codeBg.value,
            contentStyle = contentStyle,
        )
    }

    // Fast path: cache hit or short enough to parse inline.
    val syncValue: ParsedHtmlContent? = remember(cacheKey) {
        htmlCache.get(cacheKey) ?: if (html.length < HTML_SYNC_PARSE_THRESHOLD) {
            parseAndCacheHtml(cacheKey, html, linkColor, hashtagColor, mentionBg, mentionNameColor, codeBg, contentStyle)
        } else {
            null
        }
    }

    if (syncValue != null) return syncValue

    // Slow path: long HTML, cache miss. Parse off-Main.
    val asyncValue by produceState(initialValue = emptyParsedHtmlContent, cacheKey) {
        value = withContext(Dispatchers.Default) {
            htmlCache.get(cacheKey) ?: parseAndCacheHtml(
                cacheKey,
                html,
                linkColor,
                hashtagColor,
                mentionBg,
                mentionNameColor,
                codeBg,
                contentStyle,
            )
        }
    }
    return asyncValue
}

@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    fontScale: Float = 1f,
    contentStyle: HtmlContentStyle = HtmlContentStyle.Compact,
    onMentionClick: ((handle: String) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    onHeadingPositioned: ((id: String, coordinates: LayoutCoordinates) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val colors = LocalAppColors.current
    val linkColor = colors.accent
    val hashtagColor = colors.hashtag
    val mentionBg = colors.accent.copy(alpha = 0.10f)
    val mentionNameColor = colors.textSecondary
    val codeBg = colors.surface
    val textColor = colors.textBody

    val effectiveFontScale = if (fontScale != 1f) fontScale else LocalFontScale.current
    val normalizedHtml = remember(html) { normalizeHtmlForRendering(html) }
    val baseStyle = LocalAppTypography.current.bodyLarge.copy(color = textColor)
    val scaledBodyStyle = if (effectiveFontScale != 1f) {
        baseStyle.copy(fontSize = baseStyle.fontSize * effectiveFontScale)
    } else baseStyle
    val bodyStyle = scaledBodyStyle.withContentStyle(contentStyle)
    val listStyle = scaledBodyStyle.withListContentStyle(contentStyle)

    if (maxLines < Int.MAX_VALUE) {
        // Preview mode: flat AnnotatedString (no block code highlighting)
        val parsedContent = rememberParsedHtml(
            normalizedHtml,
            linkColor,
            hashtagColor,
            mentionBg,
            mentionNameColor,
            codeBg,
            contentStyle,
        )

        HtmlClickableText(
            parsedContent = parsedContent,
            style = bodyStyle,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            uriHandler = uriHandler,
            onMentionClick = onMentionClick,
            onLinkClick = onLinkClick,
            onTextClick = onTextClick,
        )
    } else {
        // Full mode: block-based rendering with syntax-highlighted code blocks
        val splitHeadings = onHeadingPositioned != null
        val blocks = remember(normalizedHtml, splitHeadings) {
            splitIntoBlocks(normalizedHtml, splitHeadings = splitHeadings)
        }

        Column(modifier = modifier) {
            blocks.forEachIndexed { index, block ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(blockSpacing(blocks[index - 1], block)))
                }
                when (block) {
                    is ContentBlock.Text -> {
                        if (block.html.isNotBlank()) {
                            val parsedContent = rememberParsedHtml(
                                block.html,
                                linkColor,
                                hashtagColor,
                                mentionBg,
                                mentionNameColor,
                                codeBg,
                                contentStyle,
                            )
                            if (parsedContent.isNotEmpty()) {
                                HtmlClickableText(
                                    parsedContent = parsedContent,
                                    style = bodyStyle,
                                    uriHandler = uriHandler,
                                    onMentionClick = onMentionClick,
                                    onLinkClick = onLinkClick,
                                    onTextClick = onTextClick,
                                )
                            }
                        }
                    }
                    is ContentBlock.List -> {
                        val parsedList = remember(block.html) { parseListHtml(block.html) }
                        if (parsedList != null) {
                            RenderListBlock(
                                block = parsedList,
                                level = 0,
                                textStyle = listStyle,
                                linkColor = linkColor,
                                hashtagColor = hashtagColor,
                                mentionBg = mentionBg,
                                mentionNameColor = mentionNameColor,
                                codeBg = codeBg,
                                contentStyle = contentStyle,
                                uriHandler = uriHandler,
                                onMentionClick = onMentionClick,
                                onLinkClick = onLinkClick,
                                onTextClick = onTextClick,
                            )
                        }
                    }
                    is ContentBlock.Code -> {
                        CodeBlockView(
                            codeHtml = block.codeHtml
                        )
                    }
                    is ContentBlock.Image -> {
                        AsyncImage(
                            model = block.src,
                            contentDescription = block.alt,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppShapes.mediaRadius))
                        )
                    }
                    is ContentBlock.Heading -> {
                        val headingContent = rememberParsedHtml(
                            block.innerHtml,
                            linkColor,
                            hashtagColor,
                            mentionBg,
                            mentionNameColor,
                            codeBg,
                            contentStyle,
                        )
                        val headingStyle = remember(bodyStyle, block.level) {
                            bodyStyle.copy(
                                fontSize = bodyStyle.fontSize * when (block.level) {
                                    1 -> 1.5f
                                    2 -> 1.3f
                                    3 -> 1.15f
                                    else -> 1.0f
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        val anchorId = block.anchorId
                        val wrapperModifier = Modifier
                            .fillMaxWidth()
                            .let { base ->
                                if (anchorId != null && onHeadingPositioned != null) {
                                    base.onGloballyPositioned { coords ->
                                        onHeadingPositioned(anchorId, coords)
                                    }
                                } else base
                            }
                        Box(modifier = wrapperModifier) {
                            if (headingContent.isNotEmpty()) {
                                HtmlClickableText(
                                    parsedContent = headingContent,
                                    style = headingStyle,
                                    uriHandler = uriHandler,
                                    onMentionClick = onMentionClick,
                                    onLinkClick = onLinkClick,
                                    onTextClick = onTextClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderListBlock(
    block: ParsedListBlock,
    level: Int,
    textStyle: TextStyle,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    mentionNameColor: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    Column {
        block.items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(if (level == 0) 9.dp else 7.5.dp))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width((level * 8).dp))
                Text(
                    text = if (block.ordered) "${index + 1}." else "\u2022",
                    style = textStyle,
                    modifier = Modifier.width(if (block.ordered) 24.dp else 14.dp)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    val normalizedItemHtml = remember(item.contentHtml) { normalizeListItemHtml(item.contentHtml) }
                    if (normalizedItemHtml.isNotBlank()) {
                        val parsedContent = rememberParsedHtml(
                            normalizedItemHtml,
                            linkColor,
                            hashtagColor,
                            mentionBg,
                            mentionNameColor,
                            codeBg,
                            contentStyle,
                        )
                        if (parsedContent.isNotEmpty()) {
                            HtmlClickableText(
                                parsedContent = parsedContent,
                                style = textStyle,
                                modifier = Modifier.fillMaxWidth(),
                                uriHandler = uriHandler,
                                onMentionClick = onMentionClick,
                                onLinkClick = onLinkClick,
                                onTextClick = onTextClick,
                            )
                        }
                    }

                    item.children.forEachIndexed { childIndex, child ->
                        Spacer(modifier = Modifier.height(if (childIndex == 0 && normalizedItemHtml.isNotBlank()) 6.5.dp else 5.dp))
                        RenderListBlock(
                            block = child,
                            level = level + 1,
                            textStyle = textStyle,
                            linkColor = linkColor,
                            hashtagColor = hashtagColor,
                            mentionBg = mentionBg,
                            mentionNameColor = mentionNameColor,
                            codeBg = codeBg,
                            contentStyle = contentStyle,
                            uriHandler = uriHandler,
                            onMentionClick = onMentionClick,
                            onLinkClick = onLinkClick,
                            onTextClick = onTextClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HtmlClickableText(
    parsedContent: ParsedHtmlContent,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)?,
) {
    var textLayoutResult by remember(parsedContent.text) { mutableStateOf<TextLayoutResult?>(null) }
    val inlineContent = remember(parsedContent.inlineImages) {
        parsedContent.inlineImages.mapValues { (_, image) ->
            InlineTextContent(
                if (image.isCustomEmoji) {
                    // Sized relative to the surrounding text, like the server's `height: 1em`.
                    Placeholder(
                        width = CUSTOM_EMOJI_SIZE_EM.em,
                        height = CUSTOM_EMOJI_SIZE_EM.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    )
                } else {
                    Placeholder(
                        width = (image.width + INLINE_IMAGE_TRAILING_SPACE).sp,
                        height = image.height.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    )
                }
            ) {
                AsyncImage(
                    model = image.src,
                    contentDescription = image.alt,
                    contentScale = if (image.isCustomEmoji) ContentScale.Fit else ContentScale.Crop,
                    modifier = if (image.isCustomEmoji) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .size(image.width.dp, image.height.dp)
                            .clip(CircleShape)
                    }
                )
            }
        }
    }

    BasicText(
        text = parsedContent.text,
        modifier = modifier.pointerInput(parsedContent.text, uriHandler, onMentionClick, onLinkClick, onTextClick) {
            detectTapGestures { position ->
                val offset = textLayoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                handleClick(parsedContent.text, offset, uriHandler, onMentionClick, onLinkClick, onTextClick)
            }
        },
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
        onTextLayout = { textLayoutResult = it },
    )
}

private fun handleClick(
    annotatedString: AnnotatedString,
    offset: Int,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onMentionClick: ((String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onTextClick: (() -> Unit)? = null
) {
    annotatedString.getStringAnnotations("MENTION", offset, offset)
        .firstOrNull()?.let { annotation ->
            val handle = extractHandleFromUrl(annotation.item)
            if (handle != null && onMentionClick != null) {
                onMentionClick(handle)
            } else {
                try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
            }
            return
        }

    annotatedString.getStringAnnotations("URL", offset, offset)
        .firstOrNull()?.let { annotation ->
            if (onLinkClick != null) {
                onLinkClick(annotation.item)
            } else {
                try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
            }
            return
        }

    // No link or mention tapped — propagate to parent
    onTextClick?.invoke()
}

@VisibleForTesting
internal fun splitIntoBlocks(html: String, splitHeadings: Boolean = false): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var lastEnd = 0
    val source = html.trim()

    for (match in PRE_CODE_REGEX.findAll(source)) {
        val before = source.substring(lastEnd, match.range.first)
        if (before.isNotBlank()) {
            blocks.addAll(splitTextAndListBlocks(before))
        }

        val codeHtml = match.groupValues[1]
        blocks.add(ContentBlock.Code(codeHtml))

        lastEnd = match.range.last + 1
    }

    val after = source.substring(lastEnd)
    if (after.isNotBlank()) {
        blocks.addAll(splitTextAndListBlocks(after))
    }

    // If no code blocks found, return the whole thing as text
    if (blocks.isEmpty()) {
        blocks.addAll(splitTextAndListBlocks(source))
    }

    val withImages = blocks.flatMap { block ->
        if (block is ContentBlock.Text) extractImageBlocks(block.html) else listOf(block)
    }

    if (!splitHeadings) return withImages

    return withImages.flatMap { block ->
        if (block is ContentBlock.Text) extractHeadingBlocks(block.html) else listOf(block)
    }
}

private fun extractImageBlocks(html: String): List<ContentBlock> {
    val out = mutableListOf<ContentBlock>()
    var cursor = 0
    for (match in IMG_BLOCK_REGEX.findAll(html)) {
        if (isInsideTag(html, match.range.first, "a")) {
            continue
        }
        // Custom emoji stay in the text flow; pulling them out as blocks would
        // split the paragraph and stretch them to full width.
        if (isCustomEmoji(parseAttributes(match.groupValues[1]))) {
            continue
        }
        if (match.range.first > cursor) {
            val before = html.substring(cursor, match.range.first)
            if (before.isNotBlank()) out.add(ContentBlock.Text(before))
        }
        val attrs = parseAttributes(match.groupValues[1])
        val src = attrs["src"]
        if (!src.isNullOrBlank()) {
            out.add(ContentBlock.Image(src = src, alt = attrs["alt"]?.takeIf { it.isNotBlank() }))
        }
        cursor = match.range.last + 1
    }
    if (cursor < html.length) {
        val tail = html.substring(cursor)
        if (tail.isNotBlank()) out.add(ContentBlock.Text(tail))
    }
    if (out.isEmpty()) out.add(ContentBlock.Text(html))
    return out
}

private fun isInsideTag(html: String, index: Int, tagName: String): Boolean {
    val beforeIndex = html.substring(0, index)
    val openTag = Regex("""<${Regex.escape(tagName)}\b[^>]*>""", RegexOption.IGNORE_CASE)
        .findAll(beforeIndex)
        .lastOrNull()
        ?.range
        ?.first
        ?: return false
    val closeTag = Regex("""</${Regex.escape(tagName)}\s*>""", RegexOption.IGNORE_CASE)
        .findAll(beforeIndex)
        .lastOrNull()
        ?.range
        ?.first
        ?: -1
    return openTag > closeTag
}

private fun extractHeadingBlocks(html: String): List<ContentBlock> {
    val out = mutableListOf<ContentBlock>()
    var cursor = 0
    for (match in HEADING_REGEX.findAll(html)) {
        if (match.range.first > cursor) {
            val before = html.substring(cursor, match.range.first)
            if (before.isNotBlank()) out.add(ContentBlock.Text(before))
        }
        val level = match.groupValues[1].toInt()
        val attrs = match.groupValues[2]
        val inner = match.groupValues[3]
        // Server prefixes heading anchors with "{docId}--{slug}" but the TOC
        // JSON only returns the bare slug, so strip the prefix to match.
        val anchorId = parseAttributes(attrs)["id"]?.substringAfter("--")
        out.add(ContentBlock.Heading(level = level, anchorId = anchorId, innerHtml = inner))
        cursor = match.range.last + 1
    }
    if (cursor < html.length) {
        val tail = html.substring(cursor)
        if (tail.isNotBlank()) out.add(ContentBlock.Text(tail))
    }
    if (out.isEmpty()) out.add(ContentBlock.Text(html))
    return out
}

@VisibleForTesting
internal fun extractHandleFromUrl(url: String): String? {
    return try {
        val uri = URI(url)
        val host = uri.host ?: return null
        val path = uri.path ?: return null
        val username = path.trimStart('/').removePrefix("@")
        if (username.isNotEmpty()) "$username@$host" else null
    } catch (_: Exception) {
        null
    }
}

@VisibleForTesting
internal fun parseHtmlToAnnotatedString(
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    codeBg: Color,
    contentStyle: HtmlContentStyle = HtmlContentStyle.Compact,
): AnnotatedString {
    return parseHtmlToContent(
        html = html,
        linkColor = linkColor,
        hashtagColor = hashtagColor,
        mentionBg = mentionBg,
        codeBg = codeBg,
        mentionNameColor = linkColor,
        contentStyle = contentStyle,
    ).text
}

@VisibleForTesting
internal fun parseHtmlToContent(
    html: String,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    codeBg: Color,
    mentionNameColor: Color = linkColor,
    contentStyle: HtmlContentStyle = HtmlContentStyle.Compact,
): ParsedHtmlContent {
    val inlineImages = mutableMapOf<String, ParsedInlineImage>()
    val text = buildAnnotatedString {
        val isProse = contentStyle == HtmlContentStyle.Prose

        // Link state
        var currentLinkType: LinkType? = null
        var hasAnnotation = false

        val spanStack = mutableListOf<SpanKind>()

        // Inline style depths (for push/pop tracking)
        var boldDepth = 0
        var italicDepth = 0
        var codeDepth = 0
        var strikeDepth = 0

        // Block state
        var preDepth = 0
        var headingLevel = 0
        var blockquoteDepth = 0

        // List state
        val listStack = mutableListOf<ListContext>()
        var insideListItem = false
        var listItemParagraphOpen = false
        var listItemJustOpened = false

        // Ruby state
        var insideRt = false
        var insideRp = false

        var hasContent = false
        var justAppendedInlineImage = false
        var pos = 0
        val source = html.trim()

        while (pos < source.length) {
            val tagMatch = TAG_REGEX.find(source, pos)

            // Text before the next tag (or remaining text)
            if (tagMatch == null || tagMatch.range.first > pos) {
                val textEnd = tagMatch?.range?.first ?: source.length
                val rawText = source.substring(pos, textEnd)
                val decoded = decodeHtmlEntities(rawText)

                if (justAppendedInlineImage && decoded.isBlank()) {
                    justAppendedInlineImage = false
                } else if (!spanStack.contains(SpanKind.INVISIBLE) && !insideRp && decoded.isNotEmpty()) {
                    if (preDepth > 0) {
                        // Preserve all whitespace in preformatted blocks
                        appendStyledText(
                            builder = this,
                            text = decoded,
                            linkType = currentLinkType,
                            linkColor = linkColor,
                            hashtagColor = hashtagColor,
                            mentionBg = mentionBg,
                            mentionNameColor = mentionNameColor,
                            isMentionName = spanStack.contains(SpanKind.MENTION_NAME),
                        )
                        hasContent = true
                        justAppendedInlineImage = false
                    } else {
                        val normalizedText = normalizeInlineWhitespace(
                            text = decoded,
                            builder = this,
                            hasContent = hasContent,
                            nextTagName = tagMatch?.groupValues?.getOrNull(2)?.lowercase()
                        )
                        if (normalizedText.isNotEmpty()) {
                            appendStyledText(
                                builder = this,
                                text = normalizedText,
                                linkType = currentLinkType,
                                linkColor = linkColor,
                                hashtagColor = hashtagColor,
                                mentionBg = mentionBg,
                                mentionNameColor = mentionNameColor,
                                isMentionName = spanStack.contains(SpanKind.MENTION_NAME),
                            )
                            if (!normalizedText.isBlank()) {
                                listItemJustOpened = false
                            }
                            hasContent = true
                            justAppendedInlineImage = false
                        }
                    }
                }

                pos = textEnd
            }

            if (tagMatch != null && tagMatch.range.first == pos) {
                val isClosing = tagMatch.groupValues[1] == "/"
                val tagName = tagMatch.groupValues[2].lowercase()
                val attrString = tagMatch.groupValues[3]

                if (!isClosing) {
                    val attrs = parseAttributes(attrString)

                    when (tagName) {
                        // Block elements
                        "p" -> {
                            if (!(insideListItem && listItemJustOpened) && hasContent) {
                                ensureTrailingNewlines(this, if (insideListItem) 1 else 2)
                            }
                        }
                        "br" -> {
                            append("\n")
                        }
                        "img" -> {
                            val inlineImage = parseInlineImage(attrs, currentLinkType)
                            if (!spanStack.contains(SpanKind.INVISIBLE) && !insideRp && inlineImage != null) {
                                val inlineId = "inline-image-${inlineImages.size}"
                                if (currentLinkType == LinkType.MENTION) {
                                    withStyle(SpanStyle(background = mentionBg)) {
                                        appendInlineContent(inlineId, INLINE_IMAGE_ALTERNATE_TEXT)
                                    }
                                } else {
                                    appendInlineContent(inlineId, INLINE_IMAGE_ALTERNATE_TEXT)
                                }
                                inlineImages[inlineId] = inlineImage
                                listItemJustOpened = false
                                hasContent = true
                                justAppendedInlineImage = true
                            }
                        }

                        // Headings
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            headingLevel = tagName[1].digitToInt()
                            if (hasContent) append("\n\n")
                            val fontSize = when (headingLevel) {
                                1 -> 1.5.em
                                2 -> 1.3.em
                                3 -> 1.15.em
                                else -> 1.0.em
                            }
                            pushStyle(SpanStyle(
                                fontSize = fontSize,
                                fontWeight = FontWeight.Bold
                            ))
                        }

                        // Preformatted / code blocks
                        "pre" -> {
                            if (hasContent) append("\n\n")
                            preDepth++
                            pushStyle(SpanStyle(
                                fontFamily = UbuntuMonoFontFamily,
                                background = codeBg,
                                fontSize = 0.875.em
                            ))
                        }

                        // Inline code
                        "code" -> {
                            codeDepth++
                            if (preDepth == 0) {
                                // Only style inline <code>, not <pre><code>
                                pushStyle(SpanStyle(
                                    fontFamily = UbuntuMonoFontFamily,
                                    background = codeBg,
                                    fontSize = 0.875.em
                                ))
                            }
                        }

                        // Bold
                        "strong", "b" -> {
                            boldDepth++
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        }

                        // Italic
                        "em", "i" -> {
                            italicDepth++
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        }

                        // Strikethrough
                        "del", "s" -> {
                            strikeDepth++
                            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        }

                        // Horizontal rule
                        "hr" -> {
                            if (hasContent) append("\n")
                            append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500")
                            append("\n")
                            hasContent = true
                        }

                        // Blockquote
                        "blockquote" -> {
                            if (hasContent) append("\n\n")
                            blockquoteDepth++
                            pushStyle(SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF6B7280)
                            ))
                        }

                        // Lists
                        "ul" -> {
                            if (hasContent) {
                                ensureTrailingNewlines(this, 1)
                            }
                            listStack.add(ListContext(ordered = false))
                        }
                        "ol" -> {
                            if (hasContent) {
                                ensureTrailingNewlines(this, 1)
                            }
                            listStack.add(ListContext(ordered = true))
                        }
                        "li" -> {
                            if (listItemParagraphOpen) {
                                pop()
                                listItemParagraphOpen = false
                            }
                            if (hasContent) {
                                ensureTrailingNewlines(this, 1)
                            }
                            val ctx = listStack.lastOrNull()
                            if (ctx != null) {
                                val nestingLevel = (listStack.size - 1).coerceAtLeast(0)
                                val marker = if (ctx.ordered) {
                                    ctx.itemIndex++
                                    "${ctx.itemIndex}."
                                } else {
                                    "\u2022"
                                }
                                val baseIndent = nestingLevel * 1.2f
                                val markerIndent = if (ctx.ordered) {
                                    0.95f + (marker.length * 0.33f)
                                } else {
                                    1.0f
                                }
                                val firstLineIndent = baseIndent.em
                                val hangingIndent = (baseIndent + markerIndent).em
                                pushStyle(
                                    ParagraphStyle(
                                        textIndent = TextIndent(
                                            firstLine = firstLineIndent,
                                            restLine = hangingIndent,
                                        )
                                    )
                                )
                                listItemParagraphOpen = true
                                if (ctx.ordered) {
                                    append("$marker ")
                                } else {
                                    append("$marker ")
                                }
                            }
                            insideListItem = true
                            listItemJustOpened = true
                            hasContent = true
                        }

                        // Links
                        "a" -> {
                            val classes = attrs["class"] ?: ""
                            val href = attrs["href"] ?: ""

                            currentLinkType = when {
                                "hashtag" in classes -> LinkType.HASHTAG
                                "mention" in classes -> LinkType.MENTION
                                href.isNotEmpty() -> LinkType.REGULAR
                                else -> null
                            }

                            if (href.isNotEmpty()) {
                                val tag = if (currentLinkType == LinkType.MENTION) "MENTION" else "URL"
                                pushStringAnnotation(tag, href)
                                hasAnnotation = true
                            }
                        }

                        // Ruby annotations
                        "ruby" -> {
                            // No special action needed for opening <ruby>
                        }
                        "rt" -> {
                            insideRt = true
                            pushStyle(SpanStyle(
                                fontSize = 0.6.em,
                                baselineShift = BaselineShift.Superscript
                            ))
                        }
                        "rp" -> {
                            insideRp = true
                        }

                        // Invisible spans
                        "span" -> {
                            val classes = attrs["class"] ?: ""
                            val spanKind = when {
                                "invisible" in classes -> SpanKind.INVISIBLE
                                currentLinkType == LinkType.MENTION && "name" in classes -> SpanKind.MENTION_NAME
                                else -> SpanKind.OTHER
                            }
                            if (spanKind == SpanKind.MENTION_NAME) {
                                ensureInlineSpace(this)
                            }
                            spanStack.add(spanKind)
                        }
                    }
                } else {
                    // Closing tags
                    when (tagName) {
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            if (headingLevel > 0) {
                                pop()
                                headingLevel = 0
                            }
                        }

                        "pre" -> {
                            if (preDepth > 0) {
                                preDepth--
                                pop()
                            }
                        }

                        "code" -> {
                            if (codeDepth > 0) {
                                codeDepth--
                                if (preDepth == 0) {
                                    // Only pop if we pushed for inline code
                                    pop()
                                }
                            }
                        }

                        "strong", "b" -> {
                            if (boldDepth > 0) {
                                boldDepth--
                                pop()
                            }
                        }

                        "em", "i" -> {
                            if (italicDepth > 0) {
                                italicDepth--
                                pop()
                            }
                        }

                        "del", "s" -> {
                            if (strikeDepth > 0) {
                                strikeDepth--
                                pop()
                            }
                        }

                        "blockquote" -> {
                            if (blockquoteDepth > 0) {
                                blockquoteDepth--
                                pop()
                            }
                        }

                        "ul", "ol" -> {
                            if (listItemParagraphOpen) {
                                pop()
                                listItemParagraphOpen = false
                            }
                            if (listStack.isNotEmpty()) {
                                // Use removeAt instead of removeLast to avoid Java 21 SequencedCollection dependency
                                listStack.removeAt(listStack.lastIndex)
                            }
                            insideListItem = false
                            if (!isProse && listStack.isEmpty() && hasContent) {
                                append("\n")
                            }
                        }

                        "li" -> {
                            if (listItemParagraphOpen) {
                                pop()
                                listItemParagraphOpen = false
                            }
                            insideListItem = false
                            listItemJustOpened = false
                        }

                        "a" -> {
                            if (hasAnnotation) {
                                pop()
                                hasAnnotation = false
                            }
                            currentLinkType = null
                        }

                        "ruby" -> {
                            // No special action needed for closing </ruby>
                        }
                        "rt" -> {
                            if (insideRt) {
                                pop()
                                insideRt = false
                            }
                        }
                        "rp" -> {
                            insideRp = false
                        }

                        "span" -> {
                            if (spanStack.isNotEmpty()) {
                                spanStack.removeAt(spanStack.lastIndex)
                            }
                        }
                    }
                }

                pos = tagMatch.range.last + 1
            }
        }
    }.trimTrailingLineBreaks()
    return ParsedHtmlContent(text = text, inlineImages = inlineImages)
}

private fun AnnotatedString.trimTrailingLineBreaks(): AnnotatedString {
    var end = text.length
    while (end > 0 && text[end - 1] == '\n') {
        end--
    }
    return if (end == text.length) this else subSequence(0, end)
}

private fun appendStyledText(
    builder: AnnotatedString.Builder,
    text: String,
    linkType: LinkType?,
    linkColor: Color,
    hashtagColor: Color,
    mentionBg: Color,
    mentionNameColor: Color,
    isMentionName: Boolean,
) {
    when (linkType) {
        LinkType.MENTION -> {
            builder.withStyle(
                SpanStyle(
                    color = if (isMentionName) mentionNameColor else linkColor,
                    fontWeight = if (isMentionName) FontWeight.Normal else FontWeight.SemiBold,
                    background = mentionBg,
                    fontSize = if (isMentionName) 0.95.em else 1.0.em,
                )
            ) {
                append(text)
            }
        }
        LinkType.HASHTAG -> {
            builder.withStyle(SpanStyle(color = hashtagColor)) {
                append(text)
            }
        }
        LinkType.REGULAR -> {
            builder.withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(text)
            }
        }
        null -> {
            builder.append(text)
        }
    }
}

private fun ensureInlineSpace(builder: AnnotatedString.Builder) {
    val previousChar = builder.toAnnotatedString().text.lastOrNull()
    if (previousChar != null && previousChar != '\n' && previousChar != ' ') {
        builder.append(" ")
    }
}

private fun parseInlineImage(
    attrs: Map<String, String>,
    linkType: LinkType?,
): ParsedInlineImage? {
    val src = attrs["src"]?.takeIf { it.isNotBlank() } ?: return null
    val classes = attrs["class"].orEmpty()
    val customEmoji = isCustomEmoji(attrs)
    if (linkType == null && "inline-block" !in classes && !customEmoji) return null

    val width = attrs["width"]
        ?.toIntOrNull()
        ?.coerceIn(INLINE_IMAGE_MIN_SIZE, INLINE_IMAGE_MAX_SIZE)
        ?: INLINE_IMAGE_DEFAULT_SIZE
    val height = attrs["height"]
        ?.toIntOrNull()
        ?.coerceIn(INLINE_IMAGE_MIN_SIZE, INLINE_IMAGE_MAX_SIZE)
        ?: width

    return ParsedInlineImage(
        src = src,
        alt = attrs["alt"]?.takeIf { it.isNotBlank() },
        width = width,
        height = height,
        isCustomEmoji = customEmoji,
    )
}

private val EMOJI_SHORTCODE_REGEX = Regex("""^:[^:\s]+:$""")

// hackers.pub serves remote custom emoji as
// `<img src=... alt=":name:" style="height:1em;vertical-align:middle;display:inline-block;">`
// with no class or size attributes; the web frontend's own markup uses `class="emoji"`.
private fun isCustomEmoji(attrs: Map<String, String>): Boolean =
    attrs["class"].orEmpty().split(' ').any { it == "emoji" } ||
        EMOJI_SHORTCODE_REGEX.matches(attrs["alt"].orEmpty().trim())

private fun TextStyle.withContentStyle(contentStyle: HtmlContentStyle): TextStyle {
    return when (contentStyle) {
        HtmlContentStyle.Compact -> this
        HtmlContentStyle.Prose -> copy(lineHeight = fontSize * 1.7f)
    }
}

private fun TextStyle.withListContentStyle(contentStyle: HtmlContentStyle): TextStyle {
    return when (contentStyle) {
        HtmlContentStyle.Compact -> this
        HtmlContentStyle.Prose -> copy(lineHeight = fontSize * 1.35f)
    }
}

private fun parseAttributes(attrString: String): Map<String, String> {
    val attrs = mutableMapOf<String, String>()
    for (match in ATTR_REGEX.findAll(attrString)) {
        attrs[match.groupValues[1]] = match.groupValues[2]
    }
    return attrs
}

@VisibleForTesting
internal fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}

@VisibleForTesting
internal fun normalizeHtmlForRendering(html: String): String {
    return html
        .replace(EMPTY_PARAGRAPH_REGEX, "")
        .replace(LIST_BOUNDARY_BREAK_REGEX, "$1")
        .replace(LIST_BREAK_BEFORE_TAG_REGEX, "$1")
}

@VisibleForTesting
internal fun normalizeListHtml(html: String): String {
    return normalizeHtmlForRendering(html)
        .replace(Regex(""">\s+<"""), "><")
}

@VisibleForTesting
internal fun normalizeListItemHtml(html: String): String {
    return normalizeHtmlForRendering(html).trim()
}

private fun normalizeInlineWhitespace(
    text: String,
    builder: AnnotatedString.Builder,
    hasContent: Boolean,
    nextTagName: String?,
): String {
    if (!text.isBlank()) return text
    if (!hasContent) return ""

    val previousChar = builder.toAnnotatedString().text.lastOrNull()
    if (previousChar == null || previousChar == '\n' || previousChar == ' ') return ""
    if (nextTagName != null && nextTagName in BLOCK_TAGS) return ""

    return " "
}

private fun endsWithLineBreak(builder: AnnotatedString.Builder): Boolean {
    return builder.toAnnotatedString().text.lastOrNull() == '\n'
}

private fun ensureTrailingNewlines(builder: AnnotatedString.Builder, count: Int) {
    val text = builder.toAnnotatedString().text
    var trailingNewlines = 0
    var idx = text.length - 1

    while (idx >= 0 && text[idx] == '\n') {
        trailingNewlines++
        idx--
    }

    repeat((count - trailingNewlines).coerceAtLeast(0)) { builder.append("\n") }
}

private fun splitTextAndListBlocks(html: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val source = html.trim()
    if (source.isBlank()) return blocks

    var listDepth = 0
    var listStart = -1
    var lastEnd = 0
    var pos = 0

    while (pos < source.length) {
        val tagMatch = TAG_REGEX.find(source, pos) ?: break
        val isClosing = tagMatch.groupValues[1] == "/"
        val tagName = tagMatch.groupValues[2].lowercase()

        if (tagName == "ul" || tagName == "ol") {
            if (!isClosing) {
                if (listDepth == 0) {
                    val before = source.substring(lastEnd, tagMatch.range.first)
                    if (before.isNotBlank()) {
                        blocks.add(ContentBlock.Text(before))
                    }
                    listStart = tagMatch.range.first
                }
                listDepth++
            } else if (listDepth > 0) {
                listDepth--
                if (listDepth == 0 && listStart >= 0) {
                    val listHtml = source.substring(listStart, tagMatch.range.last + 1)
                    if (listHtml.isNotBlank()) {
                        blocks.add(ContentBlock.List(listHtml))
                    }
                    lastEnd = tagMatch.range.last + 1
                    listStart = -1
                }
            }
        }

        pos = tagMatch.range.last + 1
    }

    val tail = source.substring(lastEnd)
    if (tail.isNotBlank()) {
        blocks.add(ContentBlock.Text(tail))
    }

    return if (blocks.isEmpty()) listOf(ContentBlock.Text(source)) else blocks
}

@VisibleForTesting
internal fun parseListHtml(html: String): ParsedListBlock? {
    val source = normalizeListHtml(html).trim()
    return parseListBlockAt(source, 0)?.first
}

private fun parseListBlockAt(html: String, start: Int): Pair<ParsedListBlock, Int>? {
    val openTag = TAG_REGEX.find(html, start) ?: return null
    if (openTag.range.first != start) return null
    if (openTag.groupValues[1] == "/") return null

    val tagName = openTag.groupValues[2].lowercase()
    if (tagName != "ul" && tagName != "ol") return null

    val ordered = tagName == "ol"
    val items = mutableListOf<ParsedListItem>()
    var pos = openTag.range.last + 1

    while (pos < html.length) {
        val nextTag = TAG_REGEX.find(html, pos) ?: break
        val isClosing = nextTag.groupValues[1] == "/"
        val nextName = nextTag.groupValues[2].lowercase()

        if (isClosing && nextName == tagName) {
            return ParsedListBlock(ordered, items) to (nextTag.range.last + 1)
        }

        if (!isClosing && nextName == "li") {
            val item = parseListItemAt(html, nextTag.range.first) ?: return null
            items.add(item.first)
            pos = item.second
            continue
        }

        pos = nextTag.range.last + 1
    }

    return ParsedListBlock(ordered, items) to pos
}

private fun parseListItemAt(html: String, start: Int): Pair<ParsedListItem, Int>? {
    val openTag = TAG_REGEX.find(html, start) ?: return null
    if (openTag.range.first != start || openTag.groupValues[1] == "/" || openTag.groupValues[2].lowercase() != "li") {
        return null
    }

    val children = mutableListOf<ParsedListBlock>()
    val contentSegments = mutableListOf<String>()
    var pos = openTag.range.last + 1
    var contentStart = pos

    while (pos < html.length) {
        val nextTag = TAG_REGEX.find(html, pos) ?: break
        val isClosing = nextTag.groupValues[1] == "/"
        val nextName = nextTag.groupValues[2].lowercase()

        if (!isClosing && (nextName == "ul" || nextName == "ol")) {
            val beforeChild = html.substring(contentStart, nextTag.range.first)
            if (beforeChild.isNotBlank()) {
                contentSegments.add(beforeChild)
            }
            val child = parseListBlockAt(html, nextTag.range.first) ?: return null
            children.add(child.first)
            pos = child.second
            contentStart = pos
            continue
        }

        if (isClosing && nextName == "li") {
            val beforeClose = html.substring(contentStart, nextTag.range.first)
            if (beforeClose.isNotBlank()) {
                contentSegments.add(beforeClose)
            }
            return ParsedListItem(
                contentHtml = contentSegments.joinToString("").trim(),
                children = children,
            ) to (nextTag.range.last + 1)
        }

        pos = nextTag.range.last + 1
    }

    return null
}

private fun blockSpacing(previous: ContentBlock, current: ContentBlock) = when {
    previous is ContentBlock.Code || current is ContentBlock.Code -> 8.dp
    previous is ContentBlock.List && current is ContentBlock.List -> 4.dp
    previous is ContentBlock.List || current is ContentBlock.List -> 16.dp
    current is ContentBlock.Heading -> 16.dp
    previous is ContentBlock.Heading -> 8.dp
    previous is ContentBlock.Image || current is ContentBlock.Image -> 12.dp
    else -> 0.dp
}
