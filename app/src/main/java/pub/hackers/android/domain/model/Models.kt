package pub.hackers.android.domain.model

import java.time.Instant

data class Actor(
    val id: String,
    val name: String?,
    val handle: String,
    val avatarUrl: String
)

data class Media(
    val url: String,
    val thumbnailUrl: String?,
    val alt: String?,
    val height: Int?,
    val width: Int?
)

data class EngagementStats(
    val replies: Int,
    val reactions: Int,
    val shares: Int,
    val quotes: Int
)

data class Post(
    val id: String,
    val typename: String,
    val name: String?,
    val published: Instant,
    val summary: String?,
    val content: String,
    val excerpt: String,
    val url: String?,
    val viewerHasShared: Boolean,
    val actor: Actor,
    val media: List<Media>,
    val engagementStats: EngagementStats,
    val mentions: List<String>,
    val sharedPost: Post? = null,
    val replyTarget: Post? = null,
    val visibility: PostVisibility = PostVisibility.PUBLIC
)

enum class PostVisibility {
    PUBLIC, UNLISTED, FOLLOWERS, DIRECT, NONE
}

sealed class Notification {
    abstract val id: String
    abstract val uuid: String
    abstract val created: Instant
    abstract val actors: List<Actor>

    data class Follow(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>
    ) : Notification()

    data class Mention(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: Post?
    ) : Notification()

    data class Reply(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: Post?
    ) : Notification()

    data class Quote(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: Post?
    ) : Notification()

    data class Share(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val post: Post?
    ) : Notification()

    data class React(
        override val id: String,
        override val uuid: String,
        override val created: Instant,
        override val actors: List<Actor>,
        val emoji: String?,
        val customEmoji: CustomEmoji?,
        val post: Post?
    ) : Notification()
}

data class CustomEmoji(
    val id: String,
    val name: String,
    val imageUrl: String
)

data class ReactionGroup(
    val emoji: String?,
    val customEmoji: CustomEmoji?,
    val count: Int,
    val reactors: List<Actor>
)

data class Viewer(
    val id: String,
    val username: String,
    val name: String,
    val bio: String,
    val avatarUrl: String,
    val handle: String
)

data class LoginChallenge(
    val token: String
)

data class Session(
    val id: String,
    val account: Account
)

data class Account(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val handle: String
)

data class TimelineResult(
    val posts: List<Post>,
    val hasNextPage: Boolean,
    val endCursor: String?
)

data class NotificationsResult(
    val notifications: List<Notification>,
    val hasNextPage: Boolean,
    val endCursor: String?
)

data class PostDetailResult(
    val post: Post,
    val reactionGroups: List<ReactionGroup>,
    val replies: List<Post>,
    val hasMoreReplies: Boolean,
    val repliesEndCursor: String?
)

data class ProfileResult(
    val actor: Actor,
    val bio: String?,
    val posts: List<Post>,
    val hasNextPage: Boolean,
    val endCursor: String?
)
