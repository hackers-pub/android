package pub.hackers.android.ui.share

import android.content.Context
import android.content.Intent

/** Opens the system share sheet with [text]. */
fun Context.sharePlainText(text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(sendIntent, null))
}
