package pub.hackers.android.ui.screens.postdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import pub.hackers.android.ui.components.CompactTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.ui.components.ErrorMessage
import pub.hackers.android.ui.components.FullScreenLoading
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.MediaGrid
import pub.hackers.android.ui.components.PostCard
import pub.hackers.android.ui.components.QuotedPostPreview
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CompactTopBar(
                title = "Post",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.post != null) {
                FloatingActionButton(
                    onClick = { onReplyClick(postId) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(R.string.reply)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    FullScreenLoading()
                }
                uiState.error != null -> {
                    ErrorMessage(
                        message = uiState.error ?: stringResource(R.string.error_generic),
                        onRetry = { viewModel.loadPost(postId) }
                    )
                }
                uiState.post != null -> {
                    PostDetailContent(
                        post = uiState.post!!,
                        reactionGroups = uiState.reactionGroups,
                        replies = uiState.replies,
                        onProfileClick = onProfileClick,
                        onPostClick = onPostClick,
                        onShareClick = {
                            if (uiState.post!!.viewerHasShared) {
                                viewModel.unsharePost()
                            } else {
                                viewModel.sharePost()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailContent(
    post: Post,
    reactionGroups: List<ReactionGroup>,
    replies: List<Post>,
    onProfileClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onShareClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())

    LazyColumn {
        item {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Reply target preview
                if (post.replyTarget != null) {
                    ReplyTargetPreview(
                        post = post.replyTarget!!,
                        onClick = { onPostClick(post.replyTarget!!.id) },
                        onProfileClick = onProfileClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = post.actor.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.actor.name ?: post.actor.handle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "@${post.actor.handle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                post.name?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HtmlContent(
                    html = post.content,
                    modifier = Modifier.fillMaxWidth()
                )

                if (post.media.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MediaGrid(media = post.media)
                }

                if (post.quotedPost != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    QuotedPostPreview(
                        post = post.quotedPost!!,
                        onClick = { onPostClick(post.quotedPost!!.id) },
                        onProfileClick = onProfileClick
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = dateFormatter.format(post.published),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${post.engagementStats.replies} ${stringResource(R.string.replies)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${post.engagementStats.shares} ${stringResource(R.string.shares)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${post.engagementStats.reactions} ${stringResource(R.string.reactions)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (reactionGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        reactionGroups.forEach { group ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (group.emoji != null) {
                                        Text(text = group.emoji)
                                    } else if (group.customEmoji != null) {
                                        AsyncImage(
                                            model = group.customEmoji.imageUrl,
                                            contentDescription = group.customEmoji.name,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = group.count.toString(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.share),
                            tint = if (post.viewerHasShared)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()
        }

        if (replies.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.replies),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            items(
                items = replies,
                key = { it.id }
            ) { reply ->
                PostCard(
                    post = reply,
                    onClick = { onPostClick(reply.id) },
                    onProfileClick = onProfileClick,
                    onQuotedPostClick = onPostClick
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun ReplyTargetPreview(
    post: Post,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick(post.actor.handle) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = post.actor.name ?: post.actor.handle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "@${post.actor.handle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HtmlContent(
            html = post.content,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        if (post.media.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            MediaGrid(media = post.media)
        }
    }
}
