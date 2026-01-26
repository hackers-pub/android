package pub.hackers.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import java.time.Duration
import java.time.Instant

@Composable
fun PostCard(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayPost = post.sharedPost ?: post
    val isRepost = post.sharedPost != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isRepost) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.actor.name ?: post.actor.handle} ${stringResource(R.string.share)}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = displayPost.actor.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick(displayPost.actor.handle) },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayPost.actor.name ?: displayPost.actor.handle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { onProfileClick(displayPost.actor.handle) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatRelativeTime(displayPost.published),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "@${displayPost.actor.handle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            displayPost.name?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            HtmlContent(
                html = displayPost.content,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )

            if (displayPost.media.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                MediaGrid(media = displayPost.media)
            }

            Spacer(modifier = Modifier.height(12.dp))

            EngagementBar(
                post = displayPost,
                onReplyClick = onReplyClick,
                onShareClick = onShareClick
            )
        }
    }
}

@Composable
private fun EngagementBar(
    post: Post,
    onReplyClick: (() -> Unit)?,
    onShareClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EngagementButton(
            icon = Icons.Outlined.ChatBubbleOutline,
            count = post.engagementStats.replies,
            contentDescription = stringResource(R.string.replies),
            onClick = onReplyClick
        )

        EngagementButton(
            icon = if (post.viewerHasShared) Icons.Filled.Repeat else Icons.Outlined.Repeat,
            count = post.engagementStats.shares,
            contentDescription = stringResource(R.string.shares),
            onClick = onShareClick,
            isActive = post.viewerHasShared
        )

        EngagementButton(
            icon = Icons.Outlined.Favorite,
            count = post.engagementStats.reactions,
            contentDescription = stringResource(R.string.reactions),
            onClick = null
        )

        EngagementButton(
            icon = Icons.Outlined.FormatQuote,
            count = post.engagementStats.quotes,
            contentDescription = stringResource(R.string.quotes),
            onClick = null
        )
    }
}

@Composable
private fun EngagementButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    contentDescription: String,
    onClick: (() -> Unit)?,
    isActive: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onClick?.invoke() },
            enabled = onClick != null
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        if (count > 0) {
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MediaGrid(media: List<pub.hackers.android.domain.model.Media>) {
    when (media.size) {
        1 -> {
            AsyncImage(
                model = media[0].url,
                contentDescription = media[0].alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        2 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                media.forEach { item ->
                    AsyncImage(
                        model = item.url,
                        contentDescription = item.alt,
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    media.take(2).forEach { item ->
                        AsyncImage(
                            model = item.url,
                            contentDescription = item.alt,
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                if (media.size > 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        media.drop(2).take(2).forEach { item ->
                            AsyncImage(
                                model = item.url,
                                contentDescription = item.alt,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HtmlContent(
    html: String,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    val plainText = html
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim()

    Text(
        text = plainText,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

private fun formatRelativeTime(instant: Instant): String {
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.toMinutes() < 1 -> "now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
        duration.toHours() < 24 -> "${duration.toHours()}h"
        duration.toDays() < 7 -> "${duration.toDays()}d"
        duration.toDays() < 30 -> "${duration.toDays() / 7}w"
        duration.toDays() < 365 -> "${duration.toDays() / 30}mo"
        else -> "${duration.toDays() / 365}y"
    }
}

private fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
        else -> String.format("%.1fM", count / 1_000_000.0)
    }
}
