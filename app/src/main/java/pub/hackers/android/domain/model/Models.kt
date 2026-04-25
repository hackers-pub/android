package pub.hackers.android.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Actor(
    val id: String,
    val name: String?,
    val handle: String,
    val avatarUrl: String,
    val bio: String? = null
)

@Immutable
data class ActorField(
    val name: String,
    val value: String
)

@Immutable
data class AccountLink(
    val name: String,
    val handle: String?,
    val icon: String,
    val url: String,
    val verified: String?
)

@Immutable
data class Media(
    val url: String,
    val thumbnailUrl: String?,
    val alt: String?,
    val height: Int?,
    val width: Int?,
    val mediaType: String? = null
) {
    val isVideo: Boolean get() = mediaType?.startsWith("video/") == true
}

@Immutable
data class EngagementStats(
    val replies: Int,
    val reactions: Int,
    val shares: Int,
    val quotes: Int
)

@Immutable
data class PostLinkImage(
    val url: String,
    val alt: String?,
    val width: Int?,
    val height: Int?
)

@Immutable
data class PostLink(
    val title: String?,
    val description: String?,
    val url: String,
    val siteName: String?,
    val author: String?,
    val image: PostLinkImage?,
    val creator: Actor?
)

@Immutable
data class Post(
    val id: String,
    val typename: String,
    val name: String?,
    val published: Instant,
    val summary: String?,
    val content: String,
    val excerpt: String,
    val url: String?,
    val iri: String? = null,
    val viewerHasShared: Boolean,
    val viewerHasBookmarked: Boolean = false,
    val actor: Actor,
    val media: List<Media>,
    val link: PostLink? = null,
    val engagementStats: EngagementStats,
    val mentions: List<String>,
    val lastSharer: Actor? = null,
    val sharersCount: Int = 0,
    val sharedPost: Post? = null,
    val replyTarget: Post? = null,
    val quotedPost: Post? = null,
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val reactionGroups: List<ReactionGroup> = emptyList()
)

enum class PostVisibility {
    PUBLIC, UNLISTED, FOLLOWERS, DIRECT, NONE
}

@Immutable
data class NotificationPost(
    val id: String,
    val content: String
)

sealed class Notification {
    abstract val id: String
    abstract val uuid: String
    abstract val created: Instant
    abstract val actors: List<Actor>

    @Immutable
    data class Follow(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>
    ) : Notification()

    @Immutable
    data class Mention(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: NotificationPost?
    ) : Notification()

    @Immutable
    data class Reply(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: NotificationPost?
    ) : Notification()

    @Immutable
    data class Quote(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: NotificationPost?
    ) : Notification()

    @Immutable
    data class Share(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: NotificationPost?
    ) : Notification()

    @Immutable
    data class React(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val emoji: String?,
        val customEmoji: CustomEmoji?,
        val post: NotificationPost?
    ) : Notification()
}

@Immutable
data class CustomEmoji(
    val id: String,
    val name: String,
    val imageUrl: String
)

@Immutable
data class ReactionGroup(
    val emoji: String?,
    val customEmoji: CustomEmoji?,
    val count: Int,
    val reactors: List<Actor>,
    val viewerHasReacted: Boolean = false
)

@Immutable
data class Viewer(
    val id: String,
    val username: String,
    val name: String,
    val bio: String,
    val avatarUrl: String,
    val handle: String
)

@Immutable
data class LoginChallenge(
    val token: String
)

@Immutable
data class Session(
    val id: String,
    val account: Account
)

@Immutable
data class Account(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val handle: String
)

@Immutable
data class Passkey(
    val id: String,
    val name: String,
    val created: String,
    val lastUsed: String?
)

@Immutable
data class PasskeyRegistrationResult(
    val verified: Boolean,
    val passkey: Passkey?
)

@Immutable
data class TimelineResult(
    val posts: List<Post>,
    val hasNextPage: Boolean,
    val endCursor: String?
)

@Immutable
data class NotificationsResult(
    val notifications: List<Notification>,
    val hasNextPage: Boolean,
    val endCursor: String?
)

@Immutable
data class PostDetailResult(
    val post: Post,
    val reactionGroups: List<ReactionGroup>,
    val replies: List<Post>,
    val hasMoreReplies: Boolean,
    val repliesEndCursor: String?,
    val toc: List<TocItem> = emptyList(),
)

@Immutable
data class TocItem(
    val id: String,
    val level: Int,
    val title: String,
    val children: List<TocItem>,
)

@Immutable
data class SharesResult(
    val actors: List<Actor>,
    val hasNextPage: Boolean,
    val endCursor: String?
)

@Immutable
data class QuotesResult(
    val posts: List<Post>,
    val hasNextPage: Boolean,
    val endCursor: String?
)

@Immutable
data class ArticleDraft(
    val id: String,
    val title: String,
    val content: String,
    val contentHtml: String,
    val tags: List<String>,
    val created: Instant,
    val updated: Instant
)

@Immutable
data class PublishedArticle(
    val id: String,
    val name: String?,
    val url: String?
)

@Immutable
data class EditableArticle(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val language: String,
    val allowLlmTranslation: Boolean
)

@Immutable
data class ProfileResult(
    val actor: Actor,
    val bio: String?,
    val fields: List<ActorField> = emptyList(),
    val accountLinks: List<AccountLink> = emptyList(),
    val isViewer: Boolean = false,
    val viewerFollows: Boolean = false,
    val followsViewer: Boolean = false,
    val viewerBlocks: Boolean = false
)

@Immutable
data class EditableAccount(
    val id: String,
    val name: String,
    val bio: String,
    val avatarUrl: String,
    val handle: String,
    val links: List<EditableAccountLink>
)

@Immutable
data class EditableAccountLink(
    val name: String,
    val url: String
)
