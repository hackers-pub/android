package pub.hackers.android.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SPAN_REGEX = Regex("""<span([^>]*)>([\s\S]*?)</span>""")
private val STYLE_ATTR_REGEX = Regex("""style=["']([^"']*)["']""")
private val SHIKI_DARK_REGEX = Regex("""--shiki-dark:\s*(#[0-9a-fA-F]{3,8})""")
private val SHIKI_LIGHT_REGEX = Regex("""(?:^|;)\s*color:\s*(#[0-9a-fA-F]{3,8})""")
private val SHIKI_DARK_FONT_STYLE_REGEX = Regex("""--shiki-dark-font-style:\s*(\w+)""")
private val SHIKI_DARK_FONT_WEIGHT_REGEX = Regex("""--shiki-dark-font-weight:\s*(\w+)""")
private val TAG_STRIP_REGEX = Regex("""<[^>]+>""")

@Composable
fun CodeBlockView(
    codeHtml: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val defaultTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    val annotatedCode = remember(codeHtml, isDark) {
        parseShikiCodeBlock(codeHtml, isDark, defaultTextColor)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = annotatedCode,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = defaultTextColor,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}

private fun parseShikiCodeBlock(
    html: String,
    isDark: Boolean,
    defaultColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        parseShikiSpans(this, html, isDark, defaultColor, depth = 0)
    }
}

private fun parseShikiSpans(
    builder: AnnotatedString.Builder,
    html: String,
    isDark: Boolean,
    defaultColor: Color,
    depth: Int
) {
    var pos = 0

    while (pos < html.length) {
        // Find the next <span or </span or <br or plain text
        val nextTag = findNextTag(html, pos)

        if (nextTag == null) {
            // Remaining text
            val text = decodeCodeEntities(html.substring(pos))
            if (text.isNotEmpty()) {
                builder.append(text)
            }
            break
        }

        // Text before the tag
        if (nextTag.start > pos) {
            val text = decodeCodeEntities(html.substring(pos, nextTag.start))
            if (text.isNotEmpty()) {
                builder.append(text)
            }
        }

        when {
            nextTag.tag == "br" -> {
                builder.append("\n")
                pos = nextTag.end
            }
            nextTag.tag == "span" && !nextTag.isClosing -> {
                // Find matching </span>
                val innerEnd = findMatchingClose(html, nextTag.end, "span")
                if (innerEnd != null) {
                    val innerHtml = html.substring(nextTag.end, innerEnd.first)
                    val style = extractSpanStyle(nextTag.attrs, isDark)

                    if (style != null) {
                        builder.withStyle(style) {
                            parseShikiSpans(this, innerHtml, isDark, defaultColor, depth + 1)
                        }
                    } else {
                        parseShikiSpans(builder, innerHtml, isDark, defaultColor, depth + 1)
                    }

                    pos = innerEnd.second
                } else {
                    pos = nextTag.end
                }
            }
            else -> {
                // Skip other tags
                pos = nextTag.end
            }
        }
    }
}

private data class TagInfo(
    val tag: String,
    val isClosing: Boolean,
    val attrs: String,
    val start: Int,
    val end: Int
)

private val TAG_FIND_REGEX = Regex("""<(/?)(\w+)([^>]*)>""")

private fun findNextTag(html: String, startPos: Int): TagInfo? {
    val match = TAG_FIND_REGEX.find(html, startPos) ?: return null
    return TagInfo(
        tag = match.groupValues[2].lowercase(),
        isClosing = match.groupValues[1] == "/",
        attrs = match.groupValues[3],
        start = match.range.first,
        end = match.range.last + 1
    )
}

private fun findMatchingClose(html: String, startPos: Int, tagName: String): Pair<Int, Int>? {
    var depth = 1
    var pos = startPos

    while (pos < html.length) {
        val tag = findNextTag(html, pos) ?: return null

        if (tag.tag == tagName) {
            if (tag.isClosing) {
                depth--
                if (depth == 0) {
                    return Pair(tag.start, tag.end)
                }
            } else {
                depth++
            }
        }

        pos = tag.end
    }

    return null
}

private fun extractSpanStyle(attrs: String, isDark: Boolean): SpanStyle? {
    val styleMatch = STYLE_ATTR_REGEX.find(attrs) ?: return null
    val styleValue = styleMatch.groupValues[1]

    val color = if (isDark) {
        SHIKI_DARK_REGEX.find(styleValue)?.groupValues?.get(1)
    } else {
        SHIKI_LIGHT_REGEX.find(styleValue)?.groupValues?.get(1)
    }

    val fontStyle = if (isDark) {
        SHIKI_DARK_FONT_STYLE_REGEX.find(styleValue)?.groupValues?.get(1)
    } else {
        null
    }

    val fontWeight = if (isDark) {
        SHIKI_DARK_FONT_WEIGHT_REGEX.find(styleValue)?.groupValues?.get(1)
    } else {
        null
    }

    if (color == null && fontStyle == null && fontWeight == null) return null

    return SpanStyle(
        color = color?.let { parseHexColor(it) } ?: Color.Unspecified,
        fontStyle = when (fontStyle) {
            "italic" -> FontStyle.Italic
            else -> null
        },
        fontWeight = when (fontWeight) {
            "bold" -> FontWeight.Bold
            else -> null
        }
    )
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        when (cleaned.length) {
            3 -> {
                val r = cleaned[0].toString().repeat(2).toInt(16)
                val g = cleaned[1].toString().repeat(2).toInt(16)
                val b = cleaned[2].toString().repeat(2).toInt(16)
                Color(r, g, b)
            }
            6 -> Color(
                cleaned.substring(0, 2).toInt(16),
                cleaned.substring(2, 4).toInt(16),
                cleaned.substring(4, 6).toInt(16)
            )
            8 -> Color(
                cleaned.substring(2, 4).toInt(16),
                cleaned.substring(4, 6).toInt(16),
                cleaned.substring(6, 8).toInt(16),
                cleaned.substring(0, 2).toInt(16)
            )
            else -> Color.Unspecified
        }
    } catch (_: Exception) {
        Color.Unspecified
    }
}

private fun decodeCodeEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
}
