package pub.hackers.android.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import java.net.URI

private enum class LinkType {
    MENTION, HASHTAG, REGULAR
}

private data class ListContext(val ordered: Boolean, var itemIndex: Int = 0)

private val TAG_REGEX = Regex("""<(/?)(\w+)([^>]*)>""")
private val ATTR_REGEX = Regex("""([\w-]+)=["']([^"']*)["']""")

@Composable
fun HtmlContent(
    html: String,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    onMentionClick: ((handle: String) -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val isDark = isSystemInDarkTheme()
    val linkColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val mentionBg = linkColor.copy(alpha = 0.10f)
    val codeBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val textColor = MaterialTheme.colorScheme.onSurface

    val annotatedString = remember(html, linkColor, mentionBg, codeBg) {
        parseHtmlToAnnotatedString(html, linkColor, mentionBg, codeBg)
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations("MENTION", offset, offset)
                .firstOrNull()?.let { annotation ->
                    val handle = extractHandleFromUrl(annotation.item)
                    if (handle != null && onMentionClick != null) {
                        onMentionClick(handle)
                    } else {
                        try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
                    }
                    return@ClickableText
                }

            annotatedString.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let { annotation ->
                    if (onLinkClick != null) {
                        onLinkClick(annotation.item)
                    } else {
                        try { uriHandler.openUri(annotation.item) } catch (_: Exception) {}
                    }
                }
        }
    )
}

private fun extractHandleFromUrl(url: String): String? {
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

private fun parseHtmlToAnnotatedString(
    html: String,
    linkColor: Color,
    mentionBg: Color,
    codeBg: Color
): AnnotatedString {
    return buildAnnotatedString {
        // Link state
        var currentLinkType: LinkType? = null
        var hasAnnotation = false

        // Invisible span state
        var insideInvisibleSpan = false
        var invisibleSpanDepth = 0

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

        var hasContent = false
        var pos = 0
        val source = html.trim()

        while (pos < source.length) {
            val tagMatch = TAG_REGEX.find(source, pos)

            // Text before the next tag (or remaining text)
            if (tagMatch == null || tagMatch.range.first > pos) {
                val textEnd = tagMatch?.range?.first ?: source.length
                val rawText = source.substring(pos, textEnd)
                val decoded = decodeHtmlEntities(rawText)

                if (!insideInvisibleSpan && decoded.isNotEmpty()) {
                    if (preDepth > 0) {
                        // Preserve all whitespace in preformatted blocks
                        appendStyledText(this, decoded, currentLinkType, linkColor, mentionBg)
                        hasContent = true
                    } else {
                        val isInterBlockWhitespace = decoded.isBlank() && decoded.contains('\n')
                        if (!isInterBlockWhitespace) {
                            appendStyledText(this, decoded, currentLinkType, linkColor, mentionBg)
                            hasContent = true
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
                            if (hasContent) append("\n\n")
                        }
                        "br" -> {
                            append("\n")
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
                                fontFamily = FontFamily.Monospace,
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
                                    fontFamily = FontFamily.Monospace,
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
                            if (hasContent && listStack.isEmpty()) append("\n")
                            listStack.add(ListContext(ordered = false))
                        }
                        "ol" -> {
                            if (hasContent && listStack.isEmpty()) append("\n")
                            listStack.add(ListContext(ordered = true))
                        }
                        "li" -> {
                            if (insideListItem) append("\n")
                            val ctx = listStack.lastOrNull()
                            val indent = "  ".repeat((listStack.size - 1).coerceAtLeast(0))
                            if (ctx != null) {
                                if (ctx.ordered) {
                                    ctx.itemIndex++
                                    append("${indent}${ctx.itemIndex}. ")
                                } else {
                                    append("${indent}\u2022 ")
                                }
                            }
                            insideListItem = true
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

                        // Invisible spans
                        "span" -> {
                            val classes = attrs["class"] ?: ""
                            if ("invisible" in classes) {
                                insideInvisibleSpan = true
                                invisibleSpanDepth++
                            }
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
                            if (listStack.isNotEmpty()) {
                                listStack.removeLast()
                            }
                            insideListItem = false
                            if (listStack.isEmpty() && hasContent) {
                                append("\n")
                            }
                        }

                        "li" -> {
                            insideListItem = false
                        }

                        "a" -> {
                            if (hasAnnotation) {
                                pop()
                                hasAnnotation = false
                            }
                            currentLinkType = null
                        }

                        "span" -> {
                            if (insideInvisibleSpan) {
                                invisibleSpanDepth--
                                if (invisibleSpanDepth <= 0) {
                                    insideInvisibleSpan = false
                                    invisibleSpanDepth = 0
                                }
                            }
                        }
                    }
                }

                pos = tagMatch.range.last + 1
            }
        }
    }
}

private fun appendStyledText(
    builder: AnnotatedString.Builder,
    text: String,
    linkType: LinkType?,
    linkColor: Color,
    mentionBg: Color
) {
    when (linkType) {
        LinkType.MENTION -> {
            builder.withStyle(
                SpanStyle(
                    color = linkColor,
                    fontWeight = FontWeight.SemiBold,
                    background = mentionBg
                )
            ) {
                append(text)
            }
        }
        LinkType.HASHTAG -> {
            builder.withStyle(SpanStyle(color = linkColor)) {
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

private fun parseAttributes(attrString: String): Map<String, String> {
    val attrs = mutableMapOf<String, String>()
    for (match in ATTR_REGEX.findAll(attrString)) {
        attrs[match.groupValues[1]] = match.groupValues[2]
    }
    return attrs
}

private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}
