@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package pub.hackers.android.ui.components

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pub.hackers.android.R
import pub.hackers.android.domain.model.Poll
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.time.Duration
import java.time.Instant
import java.util.Locale

@Composable
fun PostCard(
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
    onEditClick: ((Post) -> Unit)? = null,
    onExternalShareClick: (() -> Unit)? = null,
    onQuotedPostClick: ((String) -> Unit)? = null,
    onVotePoll: (suspend (questionId: String, optionIndices: List<Int>) -> Result<Poll>)? = null,
    contentMaxLength: Int = 0
) {
    val displayPost = post.sharedPost ?: post

    if (displayPost.typename == "Article") {
        ArticleCard(
            post = post,
            onClick = onClick,
            onProfileClick = onProfileClick,
            onReplyClick = onReplyClick,
            onShareClick = onShareClick,
            onQuoteClick = onQuoteClick,
            onReactionClick = onReactionClick,
            onReactionLongPress = onReactionLongPress,
            onBookmarkClick = onBookmarkClick,
            onPinClick = onPinClick,
            onExternalShareClick = onExternalShareClick,
            modifier = modifier
        )
    } else {
        NoteCard(
            post = post,
            onClick = onClick,
            onProfileClick = onProfileClick,
            onReplyClick = onReplyClick,
            onShareClick = onShareClick,
            onQuoteClick = onQuoteClick,
            onReactionClick = onReactionClick,
            onReactionLongPress = onReactionLongPress,
            onBookmarkClick = onBookmarkClick,
            onPinClick = onPinClick,
            onEditClick = onEditClick,
            onExternalShareClick = onExternalShareClick,
            onQuotedPostClick = onQuotedPostClick,
            onVotePoll = onVotePoll,
            contentMaxLength = contentMaxLength,
            modifier = modifier
        )
    }
}

@Composable
private fun NoteCard(
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
    onEditClick: ((Post) -> Unit)? = null,
    onExternalShareClick: (() -> Unit)? = null,
    onQuotedPostClick: ((String) -> Unit)? = null,
    onVotePoll: (suspend (questionId: String, optionIndices: List<Int>) -> Result<Poll>)? = null,
    contentMaxLength: Int = 0
) {
    val displayPost = post.sharedPost ?: post
    val isRepost = post.lastSharer != null
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val translationFailedText = stringResource(R.string.translation_failed)
    val scope = rememberCoroutineScope()

    var translatedContent by remember(displayPost.id) { mutableStateOf<String?>(null) }
    var translationError by remember(displayPost.id) { mutableStateOf<String?>(null) }
    var isTranslating by remember(displayPost.id) { mutableStateOf(false) }
    var showTranslated by remember(displayPost.id) { mutableStateOf(false) }
    var contentWarningRevealed by remember(displayPost.id) { mutableStateOf(false) }

    // Step 1: Replace Card with plain Column
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Reply target preview (faded)
        val replyTarget = displayPost.replyTarget
        if (replyTarget != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.5f)
                    .padding(bottom = 8.dp)
                    .clickable {
                        onQuotedPostClick?.invoke(replyTarget.id)
                    },
                verticalAlignment = Alignment.Top
            ) {
                ActorAvatar(
                    actor = replyTarget.actor,
                    size = AppShapes.avatarRepost,
                    contentDescription = null,
                    onClick = { onProfileClick(replyTarget.actor.handle) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    RichDisplayName(
                        name = replyTarget.actor.name,
                        fallback = replyTarget.actor.handle,
                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    HtmlContent(
                        html = replyTarget.content,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        onTextClick = {
                            onQuotedPostClick?.invoke(replyTarget.id)
                        }
                    )
                }
            }
        }

        // Repost indicator
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

        Row(
            modifier = Modifier.fillMaxWidth()
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
                // Author row — name left, visibility icon + timestamp right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
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
                            text = displayPost.actor.handle,
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(2f, fill = false)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = when (displayPost.visibility) {
                            PostVisibility.PUBLIC -> Icons.Filled.Public
                            PostVisibility.UNLISTED -> Icons.Outlined.Lock
                            PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                            PostVisibility.DIRECT -> Icons.Outlined.Lock
                            else -> Icons.Filled.Public
                        },
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatRelativeTime(displayPost.published),
                        style = typography.labelMedium,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (displayPost.typename == "Article") {
                    Text(
                        text = "Article",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

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
                    displayPost.name?.let { title ->
                        SelectionContainer {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                // Step 3: Body text color
                val truncatedContent =
                    if (contentMaxLength > 0 && displayPost.content.length > contentMaxLength) {
                        displayPost.content.take(contentMaxLength)
                    } else {
                        displayPost.content
                    }
                val isTruncated =
                    contentMaxLength > 0 && displayPost.content.length > contentMaxLength

                val translatedText = translatedContent
                if (contentVisible) {
                    if (showTranslated && translatedText != null) {
                        SelectionContainer {
                            Text(
                                text = translatedText,
                                style = typography.bodyLarge,
                                color = colors.textBody,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        HtmlContent(
                            html = truncatedContent,
                            maxLines = if (contentMaxLength > 0) Int.MAX_VALUE else 10,
                            modifier = Modifier.fillMaxWidth(),
                            onMentionClick = onProfileClick,
                            onTextClick = onClick
                        )
                    }

                    if (isTranslating) {
                        Text(
                            text = stringResource(R.string.translating),
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    val translationErrorText = translationError
                    if (translationErrorText != null) {
                        Text(
                            text = translationErrorText,
                            style = typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Inline translate link
                    if (!isTranslating && translationError == null) {
                        // Read Configuration from composition state so locale changes
                        // trigger recomposition instead of serving stale values.
                        val configuration = LocalConfiguration.current
                        Text(
                            text = if (showTranslated) stringResource(R.string.show_original) else stringResource(
                                R.string.translate
                            ),
                            style = typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable {
                                    if (showTranslated) {
                                        showTranslated = false
                                        return@clickable
                                    }

                                    translatedContent?.let {
                                        showTranslated = true
                                        return@clickable
                                    }

                                    val targetLanguageTag = androidx.core.os.ConfigurationCompat
                                        .getLocales(configuration)
                                        .get(0)?.language ?: Locale.getDefault().language
                                    scope.launch {
                                        isTranslating = true
                                        translationError = null
                                        try {
                                            val translated = translateHtmlContent(
                                                html = displayPost.content,
                                                targetLanguageTag = targetLanguageTag
                                            )
                                            translatedContent = translated
                                            showTranslated = true
                                        } catch (_: Exception) {
                                            translationError = translationFailedText
                                        } finally {
                                            isTranslating = false
                                        }
                                    }
                                }
                        )
                    }

                    if (isTruncated || (contentMaxLength == 0 && displayPost.content.length > 500)) {
                        Text(
                            text = stringResource(R.string.read_more),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Step 6: Media grid
                    if (displayPost.media.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MediaGrid(media = displayPost.media)
                    }

                    // Link preview (only when no media and no quoted post)
                    if (displayPost.media.isEmpty() && displayPost.quotedPost == null && displayPost.link != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkPreviewCard(
                            link = displayPost.link,
                            onProfileClick = onProfileClick
                        )
                    }

                    // Quoted post
                    if (displayPost.quotedPost != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        QuotedPostPreview(
                            post = displayPost.quotedPost,
                            onClick = { onQuotedPostClick?.invoke(displayPost.quotedPost.id) },
                            onProfileClick = onProfileClick
                        )
                    }

                    // Poll
                    if (displayPost.poll != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PollView(
                            poll = displayPost.poll,
                            onVote = onVotePoll?.let { vote ->
                                { indices -> vote(displayPost.id, indices) }
                            }
                        )
                    }
                }

                // Reaction groups
                if (displayPost.reactionGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayPost.reactionGroups.forEach { group ->
                            Surface(
                                shape = RoundedCornerShape(AppShapes.reactionPillRadius),
                                color = if (group.viewerHasReacted)
                                    colors.accent.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (group.emoji != null) {
                                        Text(
                                            text = group.emoji,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else if (group.customEmoji != null) {
                                        AsyncImage(
                                            model = group.customEmoji.imageUrl,
                                            contentDescription = group.customEmoji.name,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = group.count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (group.viewerHasReacted)
                                            colors.accent
                                        else
                                            colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step 4: Engagement bar
                EngagementBar(
                    post = displayPost,
                    onReplyClick = onReplyClick,
                    onShareClick = onShareClick,
                    onQuoteClick = onQuoteClick,
                    onReactionClick = onReactionClick,
                    onReactionLongPress = onReactionLongPress,
                    onBookmarkClick = onBookmarkClick,
                    onPinClick = onPinClick?.takeIf { displayPost.canPinToViewerProfile() }?.let {
                        { it(displayPost) }
                    },
                    onEditClick = onEditClick?.takeIf { displayPost.canEditNote() }?.let {
                        { it(displayPost) }
                    },
                    onExternalShareClick = onExternalShareClick
                )
            }
        }
    }
}

// Step 4: Restyled EngagementBar
@Composable
private fun EngagementBar(
    post: Post,
    onReplyClick: (() -> Unit)?,
    onShareClick: (() -> Unit)?,
    onQuoteClick: (() -> Unit)? = null,
    onReactionClick: (() -> Unit)? = null,
    onReactionLongPress: (() -> Unit)? = null,
    onBookmarkClick: (() -> Unit)? = null,
    onPinClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onExternalShareClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val isReplied = post.engagementStats.replies > 0 && post.replyTarget != null
    val isShared = post.viewerHasShared
    val isReacted = post.reactionGroups.any { it.viewerHasReacted }
    val isBookmarked = post.viewerHasBookmarked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = (-14).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reply
        EngagementButton(
            icon = Icons.Outlined.ChatBubbleOutline,
            count = post.engagementStats.replies,
            contentDescription = stringResource(R.string.replies),
            onClick = onReplyClick,
            activeColor = colors.accent,
            isActive = isReplied
        )

        // Share/Repost — tap to show repost/quote menu
        ShareEngagementButton(
            isShared = isShared,
            count = post.engagementStats.shares,
            onShareClick = onShareClick,
            onQuoteClick = if (post.viewerCanQuote) onQuoteClick else null
        )

        // Heart/React — tap to toggle ❤️, long-press for emoji picker
        ReactionEngagementButton(
            isReacted = isReacted,
            count = post.engagementStats.reactions,
            onClick = onReactionClick,
            onLongClick = onReactionLongPress
        )

        if (onBookmarkClick != null) {
            BookmarkEngagementButton(
                isBookmarked = isBookmarked,
                onClick = onBookmarkClick,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (onPinClick != null || onEditClick != null) {
            PostMoreActionMenu(
                isPinned = post.viewerHasPinned,
                onPinClick = onPinClick,
                onEditClick = onEditClick,
                modifier = Modifier.offset(x = 14.dp),
            )
        }

        // External share — always textSecondary, offset back to align right edge
        if (onExternalShareClick != null) {
            IconButton(
                onClick = onExternalShareClick,
                modifier = Modifier.offset(x = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PostMoreActionMenu(
    isPinned: Boolean,
    onPinClick: (() -> Unit)?,
    onEditClick: (() -> Unit)?,
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
            if (onEditClick != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.compose_edit)) },
                    onClick = {
                        expanded = false
                        onEditClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
            if (onPinClick != null) {
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
}

@Composable
private fun BookmarkEngagementButton(
    isBookmarked: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = LocalAppColors.current
    val tint = if (isBookmarked) colors.bookmark else colors.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = stringResource(R.string.bookmark),
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EngagementButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    contentDescription: String,
    onClick: (() -> Unit)?,
    isActive: Boolean = false,
    activeColor: Color = LocalAppColors.current.accent
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val tint = if (isActive) activeColor else colors.textSecondary

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
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = formatCount(count),
            style = typography.labelMedium,
            color = tint
        )
    }
}

@Composable
private fun ShareEngagementButton(
    isShared: Boolean,
    count: Int,
    onShareClick: (() -> Unit)?,
    onQuoteClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val tint = if (isShared) colors.share else colors.textSecondary
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onShareClick?.invoke() },
                        onLongClick = { if (onQuoteClick != null) showMenu = true }
                    )
            ) {
                Icon(
                    imageVector = if (isShared) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                    contentDescription = stringResource(R.string.shares),
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = formatCount(count),
                style = typography.labelMedium,
                color = tint
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                onClick = {
                    showMenu = false
                    onShareClick?.invoke()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            if (onQuoteClick != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.quotes)) },
                    onClick = {
                        showMenu = false
                        onQuoteClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FormatQuote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionEngagementButton(
    isReacted: Boolean,
    count: Int,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val tint = if (isReacted) colors.reaction else colors.textSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() }
                )
        ) {
            Icon(
                imageVector = if (isReacted) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                contentDescription = stringResource(R.string.reactions),
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = formatCount(count),
            style = typography.labelMedium,
            color = tint
        )
    }
}

// Step 5: Restyled QuotedPostPreview
@Composable
fun QuotedPostPreview(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    var contentWarningRevealed by remember(post.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colors.divider,
                shape = RoundedCornerShape(AppShapes.quotedPostRadius)
            )
            .clip(RoundedCornerShape(AppShapes.quotedPostRadius))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActorAvatar(
                actor = post.actor,
                size = AppShapes.avatarQuoted,
                contentDescription = null,
                onClick = { onProfileClick(post.actor.handle) },
            )

            Spacer(modifier = Modifier.width(8.dp))

            RichDisplayName(
                name = post.actor.name,
                fallback = post.actor.handle,
                style = typography.bodyLargeSemiBold,
                color = colors.textPrimary,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onProfileClick(post.actor.handle) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = post.actor.handle,
                style = typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        post.name?.let { title ->
            SelectionContainer {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        val contentWarningText = post.contentWarningText()
        val contentVisible = !post.hasContentWarning() || contentWarningRevealed
        if (post.hasContentWarning()) {
            ContentWarningNotice(
                warningText = contentWarningText,
                revealed = contentWarningRevealed,
                onToggle = { contentWarningRevealed = !contentWarningRevealed },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (contentVisible) {
            HtmlContent(
                html = post.content,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                onMentionClick = onProfileClick,
                onTextClick = onClick
            )

            if (post.media.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val firstMedium = post.media.first()
                AsyncImage(
                    model = firstMedium.thumbnailUrl ?: firstMedium.url,
                    contentDescription = firstMedium.alt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(AppShapes.mediaRadius)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun GridMediaItem(item: pub.hackers.android.domain.model.Media, modifier: Modifier) {
    MediaImage(
        url = item.url,
        alt = item.alt,
        modifier = modifier,
        isVideo = item.isVideo,
        thumbnailUrl = item.thumbnailUrl
    )
}

// Step 6: MediaGrid with 8dp radius
@Composable
fun MediaGrid(media: List<pub.hackers.android.domain.model.Media>) {
    val gridHeight = 200.dp
    val gap = 4.dp

    when (media.size) {
        0 -> {}
        1 -> {
            GridMediaItem(
                media[0], Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .clip(RoundedCornerShape(AppShapes.mediaRadius))
            )
        }

        2 -> {
            // a | b
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth()
            ) {
                GridMediaItem(
                    media[0],
                    Modifier
                        .weight(1f)
                        .height(gridHeight)
                        .clip(RoundedCornerShape(AppShapes.mediaRadius))
                )
                GridMediaItem(
                    media[1],
                    Modifier
                        .weight(1f)
                        .height(gridHeight)
                        .clip(RoundedCornerShape(AppShapes.mediaRadius))
                )
            }
        }

        3 -> {
            // a | (b / c)
            val halfHeight = (gridHeight - gap) / 2
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth()
            ) {
                GridMediaItem(
                    media[0],
                    Modifier
                        .weight(1f)
                        .height(gridHeight)
                        .clip(RoundedCornerShape(AppShapes.mediaRadius))
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.weight(1f)
                ) {
                    GridMediaItem(
                        media[1],
                        Modifier
                            .fillMaxWidth()
                            .height(halfHeight)
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
                    )
                    GridMediaItem(
                        media[2],
                        Modifier
                            .fillMaxWidth()
                            .height(halfHeight)
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
                    )
                }
            }
        }

        else -> {
            // (a / c) | (b / d+)
            val remaining = media.size - 4
            val halfHeight = (gridHeight - gap) / 2
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.weight(1f)
                ) {
                    GridMediaItem(
                        media[0],
                        Modifier
                            .fillMaxWidth()
                            .height(halfHeight)
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
                    )
                    GridMediaItem(
                        media[2],
                        Modifier
                            .fillMaxWidth()
                            .height(halfHeight)
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.weight(1f)
                ) {
                    GridMediaItem(
                        media[1],
                        Modifier
                            .fillMaxWidth()
                            .height(halfHeight)
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(halfHeight)
                            .clip(RoundedCornerShape(AppShapes.mediaRadius))
                    ) {
                        GridMediaItem(media[3], Modifier.fillMaxSize())
                        if (remaining > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "+$remaining",
                                    color = Color.White,
                                    style = LocalAppTypography.current.titleLarge
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
fun MediaImage(
    url: String,
    alt: String?,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    thumbnailUrl: String? = null
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var showPreview by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AsyncImage(
            // Prefer thumbnailUrl for grid display (smaller payload, faster decode).
            // Full-resolution `url` is used by MediaPreviewDialog below on tap.
            model = thumbnailUrl ?: url,
            contentDescription = alt,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = colors.divider,
                    shape = RoundedCornerShape(AppShapes.mediaRadius)
                )
                .clickable {
                    if (isVideo) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.setDataAndType(Uri.parse(url), "video/*")
                        context.startActivity(intent)
                    } else {
                        showPreview = true
                    }
                },
            contentScale = ContentScale.Crop
        )

        if (isVideo) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    if (showPreview) {
        MediaPreviewDialog(
            url = url,
            alt = alt,
            onDismiss = { showPreview = false }
        )
    }
}

@Composable
private fun MediaPreviewDialog(
    url: String,
    alt: String?,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (scale <= 1f) onDismiss() }
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = Color.White
                )
            }

            // Zoomable image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = alt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .transformable(state = transformState)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showMenu = true }
                        ),
                    contentScale = ContentScale.Fit
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.download)) },
                        onClick = {
                            showMenu = false
                            val request = android.app.DownloadManager.Request(Uri.parse(url))
                                .setNotificationVisibility(
                                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                )
                                .setDestinationInExternalPublicDir(
                                    android.os.Environment.DIRECTORY_DOWNLOADS,
                                    url.substringAfterLast('/')
                                )
                            val dm =
                                context.getSystemService(android.content.Context.DOWNLOAD_SERVICE)
                                        as android.app.DownloadManager
                            dm.enqueue(request)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            showMenu = false
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, url)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            // Alt text
            if (!alt.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 16.dp, end = 64.dp, top = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = alt,
                        style = typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

internal fun formatRelativeTime(instant: Instant): String {
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
        count < 1_000_000 -> String.format(Locale.ROOT, "%.1fK", count / 1000.0)
        else -> String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0)
    }
}

private suspend fun translateHtmlContent(
    html: String,
    targetLanguageTag: String
): String = withContext(Dispatchers.IO) {
    val plainText = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .trim()

    if (plainText.isBlank()) {
        return@withContext ""
    }

    val languageIdentifier = LanguageIdentification.getClient()
    val detectedLanguageTag = languageIdentifier.identifyLanguage(plainText).await()
    val sourceLanguage = TranslateLanguage.fromLanguageTag(detectedLanguageTag)
    val targetLanguage = TranslateLanguage.fromLanguageTag(targetLanguageTag)

    if (sourceLanguage == null || targetLanguage == null || sourceLanguage == targetLanguage) {
        languageIdentifier.close()
        return@withContext plainText
    }

    val translatorOptions = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLanguage)
        .setTargetLanguage(targetLanguage)
        .build()

    val translator = Translation.getClient(translatorOptions)

    try {
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        translator.translate(plainText).await()
    } finally {
        translator.close()
        languageIdentifier.close()
    }
}
