package pub.hackers.android.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SelectionCapturingClipboardTest {

    private val system = RecordingClipboard()
    private var time = 0L
    private val clipboard = SelectionCapturingClipboard(system, now = { time })

    @Test
    fun `copies pass through to the system clipboard`() = runTest {
        clipboard.setClipEntry(entry("copied"))

        assertEquals("copied", system.text)
    }

    @Test
    fun `an armed capture takes the next copy instead of the system clipboard`() = runTest {
        val captured = mutableListOf<String>()
        clipboard.captureNextCopy { captured += it }

        clipboard.setClipEntry(entry("selected text"))

        assertEquals(listOf("selected text"), captured)
        assertNull(system.text)
    }

    @Test
    fun `a capture only applies to one copy`() = runTest {
        clipboard.captureNextCopy { }
        clipboard.setClipEntry(entry("for sharing"))

        clipboard.setClipEntry(entry("real copy"))

        assertEquals("real copy", system.text)
    }

    @Test
    fun `an expired capture does not swallow a later real copy`() = runTest {
        val captured = mutableListOf<String>()
        clipboard.captureNextCopy { captured += it }
        time += 5_000

        clipboard.setClipEntry(entry("real copy"))

        assertEquals(emptyList<String>(), captured)
        assertEquals("real copy", system.text)
    }

    private fun entry(text: String) = ClipEntry(ClipData.newPlainText("", text))

    private class RecordingClipboard : Clipboard {
        var text: String? = null
            private set

        override suspend fun getClipEntry(): ClipEntry? = null

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            text = clipEntry?.clipData?.getItemAt(0)?.text?.toString()
        }

        override val nativeClipboard: ClipboardManager
            get() = throw UnsupportedOperationException()
    }
}
