package pub.hackers.android.ui.share

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post

/**
 * Share icon that shares [post]'s link: `『title』` + URL for articles, the bare
 * URL for notes. Sharing text goes through the text-selection menu instead
 * (see [PostShareSelectionArea]). Hidden when the post has no URL to share.
 */
@Composable
fun PostShareButton(
    post: Post,
    onShare: (String) -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp? = null,
) {
    val url = post.shareUrl() ?: return
    val title = post.shareTitle()
    IconButton(
        onClick = { onShare(buildLinkShareText(title, url)) },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = stringResource(R.string.share_post_link),
            tint = tint,
            modifier = if (iconSize != null) Modifier.size(iconSize) else Modifier
        )
    }
}
