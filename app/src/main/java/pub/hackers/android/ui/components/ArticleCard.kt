package pub.hackers.android.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.ui.share.PostShareButton
import pub.hackers.android.ui.share.PostShareSelectionArea
import pub.hackers.android.ui.share.shareTitle
import pub.hackers.android.ui.share.shareUrl
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun ArticleCard(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReplyClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onQuoteClick: (() -> Unit)? = null,
    onReactionClick: (() -> Unit)? = null,
    onReactionLongPress: (() -> Unit)? = null,
    onBookmarkClick: (() -> Unit)? = null,
    onPinClick: ((Post) -> Unit)? = null,
    onExternalShare: ((String) -> Unit)? = null
) {
    val displayPost = post.sharedPost ?: post
    val isRepost = post.lastSharer != null
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    var contentWarningRevealed by remember(displayPost.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Repost indicator (matching NoteCard style)
                if (isRepost) {
                    val sharer = post.lastSharer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 54.dp, bottom = 8.dp)
                            .clickable { onProfileClick(sharer.handle) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = colors.share,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (sharer.name != null) {
                            RichDisplayName(
                                name = sharer.name,
                                fallback = sharer.handle,
                                style = typography.caption.copy(fontStyle = FontStyle.Italic),
                                color = colors.textSecondary,
                                emojiHeight = 14.dp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = sharer.handle,
                            style = typography.caption.copy(fontStyle = FontStyle.Italic),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.share) + "d",
                            style = typography.caption.copy(fontStyle = FontStyle.Italic),
                            color = colors.textSecondary
                        )
                    }
                }

                // Author row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActorAvatar(
                        actor = displayPost.actor,
                        overlayActor = post.lastSharer,
                        size = AppShapes.avatarTimeline,
                        contentDescription = "Avatar",
                        onClick = { onProfileClick(displayPost.actor.handle) },
                        isPinned = displayPost.viewerHasPinned,
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RichDisplayName(
                                name = displayPost.actor.name,
                                fallback = displayPost.actor.handle,
                                style = typography.bodyLargeSemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { onProfileClick(displayPost.actor.handle) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatRelativeTime(displayPost.published),
                                style = typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }

                        Text(
                            text = displayPost.actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                }

                Spacer(modifier = Modifier.height(12.dp))

                val contentWarningText = displayPost.contentWarningText()
                val contentVisible = !displayPost.hasContentWarning() || contentWarningRevealed
                if (displayPost.hasContentWarning()) {
                    ContentWarningNotice(
                        warningText = contentWarningText,
                        revealed = contentWarningRevealed,
                        onToggle = { contentWarningRevealed = !contentWarningRevealed },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (contentVisible) {
                    PostShareSelectionArea(
                        title = displayPost.shareTitle(),
                        url = displayPost.shareUrl(),
                        onShare = onExternalShare,
                    ) {
                        SelectionContainer {
                            Column {
                                // Article title
                                displayPost.name?.let { title ->
                                    Text(
                                        text = title,
                                        style = typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Summary or excerpt
                                val summaryText = displayPost.summary ?: displayPost.excerpt
                                if (summaryText.isNotBlank()) {
                                    Text(
                                        text = summaryText,
                                        style = typography.bodyMedium,
                                        color = colors.textSecondary,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // "Read full article" footer
            HorizontalDivider(color = colors.divider)
            Text(
                text = stringResource(R.string.read_full_article),
                style = typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface.copy(alpha = 0.5f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            // Engagement bar
            HorizontalDivider(color = colors.divider)
            ArticleEngagementBar(
                post = displayPost,
                onReplyClick = onReplyClick,
                onShareClick = onShareClick,
                onReactionClick = onReactionClick,
                onReactionLongPress = onReactionLongPress,
                onBookmarkClick = onBookmarkClick,
                onPinClick = onPinClick?.takeIf { displayPost.canPinToViewerProfile() }?.let {
                    { it(displayPost) }
                },
                onExternalShare = onExternalShare
            )
        }
    }
}

@Composable
private fun PostPinActionMenu(
    isPinned: Boolean,
    onPinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more_actions),
                tint = LocalAppColors.current.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (isPinned) R.string.unpin_post else R.string.pin_post))
                },
                onClick = {
                    expanded = false
                    onPinClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticleEngagementBar(
    post: Post,
    onReplyClick: (() -> Unit)?,
    onShareClick: (() -> Unit)?,
    onReactionClick: (() -> Unit)?,
    onReactionLongPress: (() -> Unit)?,
    onBookmarkClick: (() -> Unit)?,
    onPinClick: (() -> Unit)?,
    onExternalShare: ((String) -> Unit)?
) {
    val colors = LocalAppColors.current
    val isShared = post.viewerHasShared
    val isReacted = post.reactionGroups.any { it.viewerHasReacted }
    val isBookmarked = post.viewerHasBookmarked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(onClick = { onReplyClick?.invoke() }, enabled = onReplyClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = stringResource(R.string.replies),
                tint = colors.textSecondary
            )
        }
        IconButton(onClick = { onShareClick?.invoke() }, enabled = onShareClick != null) {
            Icon(
                imageVector = Icons.Filled.Repeat,
                contentDescription = stringResource(R.string.share),
                tint = if (isShared) colors.share else colors.textSecondary
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { onReactionClick?.invoke() },
                    onLongClick = { onReactionLongPress?.invoke() }
                )
        ) {
            Icon(
                imageVector = if (isReacted) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                contentDescription = stringResource(R.string.reactions),
                tint = if (isReacted) colors.reaction else colors.textSecondary
            )
        }
        if (onBookmarkClick != null) {
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.bookmark),
                    tint = if (isBookmarked) colors.bookmark else colors.textSecondary
                )
            }
        }
        if (onPinClick != null) {
            PostPinActionMenu(
                isPinned = post.viewerHasPinned,
                onPinClick = onPinClick,
            )
        }
        if (onExternalShare != null) {
            PostShareButton(
                post = post,
                onShare = onExternalShare,
                tint = colors.textSecondary
            )
        }
    }
}
