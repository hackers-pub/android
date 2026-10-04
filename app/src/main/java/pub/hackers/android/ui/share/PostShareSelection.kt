package pub.hackers.android.ui.share

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import pub.hackers.android.R

/**
 * Adds "Share" to the text-selection menu of every SelectionContainer inside
 * [content], sharing the selected text as an excerpt of the post.
 *
 * Compose doesn't expose the selected text to menu items. SelectionContainer's
 * own Copy item, though, hands the selection to `LocalClipboard`. So this area
 * remembers that Copy item, provides a clipboard that can capture the next copy,
 * and has Share run Copy with capture armed: the selection reaches the share
 * sheet without ever touching the system clipboard.
 *
 * Without [url] or [onShare] the menu is left untouched.
 */
@Composable
fun PostShareSelectionArea(
    title: String?,
    url: String?,
    onShare: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (url == null || onShare == null) {
        Box(modifier = modifier) { content() }
        return
    }

    val shareLabel = stringResource(R.string.share_selection)
    val currentOnShare by rememberUpdatedState(onShare)
    val currentTitle by rememberUpdatedState(title)
    val currentUrl by rememberUpdatedState(url)
    val systemClipboard = LocalClipboard.current
    val clipboard = remember(systemClipboard) { SelectionCapturingClipboard(systemClipboard) }
    val copyItem = remember { CopyItemHolder() }

    Box(
        modifier = modifier
            .filterTextContextMenuComponents { component ->
                if (component is TextContextMenuItem && component.key == TextContextMenuKeys.CopyKey) {
                    copyItem.onClick = component.onClick
                }
                true
            }
            .appendTextContextMenuComponents {
                item(key = ShareSelectionKey, label = shareLabel) {
                    val session = this
                    val copy = copyItem.onClick
                    if (copy == null) {
                        session.close()
                        return@item
                    }
                    clipboard.captureNextCopy { selection ->
                        currentOnShare(
                            buildSelectionShareText(
                                title = currentTitle,
                                selection = selection,
                                url = currentUrl,
                            )
                        )
                    }
                    copy(session)
                    session.close()
                }
            }
    ) {
        CompositionLocalProvider(LocalClipboard provides clipboard) {
            content()
        }
    }
}

private object ShareSelectionKey

private class CopyItemHolder {
    var onClick: ((TextContextMenuSession) -> Unit)? = null
}

/**
 * Passes everything through to [delegate], except that right after
 * [captureNextCopy] the next write is diverted to the callback instead of the
 * system clipboard. The capture expires quickly so that, should Copy turn out to
 * have nothing to write, the user's next real copy isn't swallowed.
 */
internal class SelectionCapturingClipboard(
    private val delegate: Clipboard,
    private val now: () -> Long = SystemClock::uptimeMillis,
) : Clipboard {
    private var onCaptured: ((String) -> Unit)? = null
    private var capturedUntil = 0L

    fun captureNextCopy(onCaptured: (String) -> Unit) {
        this.onCaptured = onCaptured
        capturedUntil = now() + CAPTURE_WINDOW_MS
    }

    override suspend fun getClipEntry(): ClipEntry? = delegate.getClipEntry()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val capture = onCaptured?.takeIf { now() <= capturedUntil }
        onCaptured = null
        if (capture == null) {
            delegate.setClipEntry(clipEntry)
            return
        }
        val clipData = clipEntry?.clipData ?: return
        if (clipData.itemCount == 0) return
        val text = clipData.getItemAt(0).text?.toString()
        if (!text.isNullOrBlank()) capture(text)
    }

    override val nativeClipboard get() = delegate.nativeClipboard

    private companion object {
        const val CAPTURE_WINDOW_MS = 1_000L
    }
}
