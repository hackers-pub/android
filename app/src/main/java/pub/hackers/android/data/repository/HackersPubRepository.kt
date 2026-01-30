package pub.hackers.android.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import pub.hackers.android.domain.model.*
import pub.hackers.android.graphql.*
import pub.hackers.android.graphql.fragment.ActorFields
import pub.hackers.android.graphql.fragment.EngagementStatsFields
import pub.hackers.android.graphql.fragment.MediaFields
import pub.hackers.android.graphql.fragment.PostFields
import pub.hackers.android.graphql.fragment.SharedPostFields
import pub.hackers.android.graphql.type.PostVisibility as GqlPostVisibility
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HackersPubRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getPublicTimeline(after: String? = null, refresh: Boolean = false): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(PublicTimelineQuery(Optional.presentIfNotNull(after)))
                .apply { if (refresh) fetchPolicy(FetchPolicy.NetworkOnly) }
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data?.publicTimeline
                Result.success(
                    TimelineResult(
                        posts = data?.edges?.mapNotNull { edge ->
                            edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                        } ?: emptyList(),
                        hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                        endCursor = data?.pageInfo?.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalTimeline(after: String? = null, refresh: Boolean = false): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(LocalTimelineQuery(Optional.presentIfNotNull(after)))
                .apply { if (refresh) fetchPolicy(FetchPolicy.NetworkOnly) }
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data?.publicTimeline
                Result.success(
                    TimelineResult(
                        posts = data?.edges?.mapNotNull { edge ->
                            edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                        } ?: emptyList(),
                        hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                        endCursor = data?.pageInfo?.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPersonalTimeline(after: String? = null, refresh: Boolean = false): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(PersonalTimelineQuery(Optional.presentIfNotNull(after)))
                .apply { if (refresh) fetchPolicy(FetchPolicy.NetworkOnly) }
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data?.personalTimeline
                Result.success(
                    TimelineResult(
                        posts = data?.edges?.mapNotNull { edge ->
                            edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                        } ?: emptyList(),
                        hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                        endCursor = data?.pageInfo?.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(after: String? = null, refresh: Boolean = false): Result<NotificationsResult> {
        return try {
            val response = apolloClient.query(NotificationsQuery(Optional.presentIfNotNull(after)))
                .apply { if (refresh) fetchPolicy(FetchPolicy.NetworkOnly) }
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data?.viewer?.notifications
                Result.success(
                    NotificationsResult(
                        notifications = data?.edges?.mapNotNull { edge ->
                            edge.node.toNotification()
                        } ?: emptyList(),
                        hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                        endCursor = data?.pageInfo?.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPosts(query: String): Result<List<Post>> {
        return try {
            val response = apolloClient.query(SearchPostQuery(query)).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val posts = response.data?.searchPost?.edges?.mapNotNull { edge ->
                    edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                } ?: emptyList()
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostDetail(id: String, repliesAfter: String? = null): Result<PostDetailResult> {
        return try {
            val response = apolloClient.query(
                PostDetailQuery(id, Optional.presentIfNotNull(repliesAfter))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val node = response.data?.node?.onPost
                    ?: return Result.failure(Exception("Post not found"))

                val post = node.postFields.toPost(
                    sharedPost = node.sharedPost?.sharedPostFields?.toPost(),
                    replyTarget = node.replyTarget?.postFields?.toPost(),
                    visibility = node.visibility.toPostVisibility()
                )

                val reactionGroups = node.reactionGroups.mapNotNull { group ->
                    when {
                        group.onEmojiReactionGroup != null -> ReactionGroup(
                            emoji = group.onEmojiReactionGroup.emoji,
                            customEmoji = null,
                            count = group.onEmojiReactionGroup.reactors.totalCount,
                            reactors = group.onEmojiReactionGroup.reactors.edges.map {
                                it.node.actorFields.toActor()
                            }
                        )
                        group.onCustomEmojiReactionGroup != null -> ReactionGroup(
                            emoji = null,
                            customEmoji = CustomEmoji(
                                id = group.onCustomEmojiReactionGroup.customEmoji.id,
                                name = group.onCustomEmojiReactionGroup.customEmoji.name,
                                imageUrl = group.onCustomEmojiReactionGroup.customEmoji.imageUrl
                            ),
                            count = group.onCustomEmojiReactionGroup.reactors.totalCount,
                            reactors = group.onCustomEmojiReactionGroup.reactors.edges.map {
                                it.node.actorFields.toActor()
                            }
                        )
                        else -> null
                    }
                }

                val replies = node.replies.edges.mapNotNull { edge ->
                    edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                }

                Result.success(
                    PostDetailResult(
                        post = post,
                        reactionGroups = reactionGroups,
                        replies = replies,
                        hasMoreReplies = node.replies.pageInfo.hasNextPage,
                        repliesEndCursor = node.replies.pageInfo.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(handle: String, postsAfter: String? = null): Result<ProfileResult> {
        return try {
            val response = apolloClient.query(
                ActorByHandleQuery(handle, Optional.presentIfNotNull(postsAfter))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val actor = response.data?.actorByHandle
                    ?: return Result.failure(Exception("Actor not found"))

                Result.success(
                    ProfileResult(
                        actor = Actor(
                            id = actor.id,
                            name = actor.name?.toString(),
                            handle = actor.handle,
                            avatarUrl = actor.avatarUrl.toString()
                        ),
                        bio = actor.bio?.toString(),
                        posts = actor.posts.edges.mapNotNull { edge ->
                            edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                        },
                        hasNextPage = actor.posts.pageInfo.hasNextPage,
                        endCursor = actor.posts.pageInfo.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getViewer(): Result<Viewer?> {
        return try {
            val response = apolloClient.query(ViewerQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val viewer = response.data?.viewer
                Result.success(
                    viewer?.let {
                        Viewer(
                            id = it.id,
                            username = it.username,
                            name = it.name,
                            bio = it.bio.toString(),
                            avatarUrl = it.avatarUrl.toString(),
                            handle = it.handle
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginByUsername(username: String, locale: String = "en"): Result<LoginChallenge> {
        return try {
            val response = apolloClient.mutation(
                LoginByUsernameMutation(
                    username = username,
                    locale = locale,
                    verifyUrl = "hackerspub://verify?token={token}&code={code}"
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.loginByUsername
                when {
                    result?.onLoginChallenge != null -> {
                        Result.success(LoginChallenge(result.onLoginChallenge.token.toString()))
                    }
                    result?.onAccountNotFoundError != null -> {
                        Result.failure(Exception("Account not found: ${result.onAccountNotFoundError.query}"))
                    }
                    else -> Result.failure(Exception("Unknown login result"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeLoginChallenge(token: String, code: String): Result<Session> {
        return try {
            val response = apolloClient.mutation(
                CompleteLoginChallengeMutation(token = token, code = code)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val session = response.data?.completeLoginChallenge
                    ?: return Result.failure(Exception("Invalid verification code"))

                Result.success(
                    Session(
                        id = session.id.toString(),
                        account = Account(
                            id = session.account.id,
                            username = session.account.username,
                            name = session.account.name,
                            avatarUrl = session.account.avatarUrl.toString(),
                            handle = session.account.handle
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNote(
        content: String,
        language: String = "en",
        visibility: PostVisibility = PostVisibility.PUBLIC,
        replyTargetId: String? = null
    ): Result<Post> {
        return try {
            val gqlVisibility = when (visibility) {
                PostVisibility.PUBLIC -> GqlPostVisibility.PUBLIC
                PostVisibility.UNLISTED -> GqlPostVisibility.UNLISTED
                PostVisibility.FOLLOWERS -> GqlPostVisibility.FOLLOWERS
                PostVisibility.DIRECT -> GqlPostVisibility.DIRECT
                PostVisibility.NONE -> GqlPostVisibility.NONE
            }

            val response = apolloClient.mutation(
                CreateNoteMutation(
                    content = content,
                    language = language,
                    visibility = gqlVisibility,
                    replyTargetId = Optional.presentIfNotNull(replyTargetId)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.createNote
                when {
                    result?.onCreateNotePayload != null -> {
                        val note = result.onCreateNotePayload.note
                        Result.success(
                            Post(
                                id = note.id,
                                typename = "Note",
                                name = null,
                                published = Instant.parse(note.published.toString()),
                                summary = null,
                                content = note.content.toString(),
                                excerpt = "",
                                url = null,
                                viewerHasShared = false,
                                actor = Actor("", null, "", ""),
                                media = emptyList(),
                                engagementStats = EngagementStats(0, 0, 0, 0),
                                mentions = emptyList()
                            )
                        )
                    }
                    result?.onInvalidInputError != null -> {
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    }
                    result?.onNotAuthenticatedError != null -> {
                        Result.failure(Exception("Not authenticated"))
                    }
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sharePost(postId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(SharePostMutation(postId)).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.sharePost
                when {
                    result?.onSharePostPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null -> {
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    }
                    result?.onNotAuthenticatedError != null -> {
                        Result.failure(Exception("Not authenticated"))
                    }
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsharePost(postId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(UnsharePostMutation(postId)).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.unsharePost
                when {
                    result?.onUnsharePostPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null -> {
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    }
                    result?.onNotAuthenticatedError != null -> {
                        Result.failure(Exception("Not authenticated"))
                    }
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeSession(sessionId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(RevokeSessionMutation(sessionId)).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchActorsByHandle(prefix: String, limit: Int = 10): Result<List<Actor>> {
        return try {
            val response = apolloClient.query(
                SearchActorsByHandleQuery(prefix = prefix, limit = Optional.present(limit))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val actors = response.data?.searchActorsByHandle?.map { actor ->
                    Actor(
                        id = actor.id,
                        name = actor.name?.toString(),
                        handle = actor.handle,
                        avatarUrl = actor.avatarUrl.toString()
                    )
                } ?: emptyList()
                Result.success(actors)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions to convert GraphQL fragment types to domain models
    private fun PostFields.toPost(
        sharedPost: Post? = null,
        replyTarget: Post? = null,
        visibility: PostVisibility = PostVisibility.PUBLIC
    ): Post {
        return Post(
            id = id,
            typename = __typename,
            name = name,
            published = Instant.parse(published.toString()),
            summary = summary,
            content = content.toString(),
            excerpt = excerpt,
            url = url?.toString(),
            viewerHasShared = viewerHasShared,
            actor = actor.actorFields.toActor(),
            media = media.map { it.mediaFields.toMedia() },
            engagementStats = engagementStats.engagementStatsFields.toEngagementStats(),
            mentions = mentions.edges.map { it.node.handle },
            sharedPost = sharedPost,
            replyTarget = replyTarget,
            quotedPost = quotedPost?.sharedPostFields?.toPost(),
            visibility = visibility
        )
    }

    private fun SharedPostFields.toPost(): Post {
        return Post(
            id = id,
            typename = __typename,
            name = name,
            published = Instant.parse(published.toString()),
            summary = summary,
            content = content.toString(),
            excerpt = excerpt,
            url = url?.toString(),
            viewerHasShared = viewerHasShared,
            actor = actor.actorFields.toActor(),
            media = media.map { it.mediaFields.toMedia() },
            engagementStats = engagementStats.engagementStatsFields.toEngagementStats(),
            mentions = mentions.edges.map { it.node.handle }
        )
    }

    private fun ActorFields.toActor(): Actor {
        return Actor(
            id = id,
            name = name?.toString(),
            handle = handle,
            avatarUrl = avatarUrl.toString()
        )
    }

    private fun MediaFields.toMedia(): Media {
        return Media(
            url = url.toString(),
            thumbnailUrl = thumbnailUrl,
            alt = alt,
            height = height,
            width = width
        )
    }

    private fun EngagementStatsFields.toEngagementStats(): EngagementStats {
        return EngagementStats(
            replies = replies,
            reactions = reactions,
            shares = shares,
            quotes = quotes
        )
    }

    private fun NotificationsQuery.Node.toNotification(): Notification? {
        val actors = actors.edges.map { it.node.actorFields.toActor() }
        val created = Instant.parse(created.toString())

        return when {
            onFollowNotification != null -> Notification.Follow(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors
            )
            onMentionNotification != null -> Notification.Mention(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onMentionNotification.post?.postFields?.toPost()
            )
            onReplyNotification != null -> Notification.Reply(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onReplyNotification.post?.postFields?.toPost()
            )
            onQuoteNotification != null -> Notification.Quote(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onQuoteNotification.post?.postFields?.toPost()
            )
            onShareNotification != null -> Notification.Share(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onShareNotification.post?.postFields?.toPost()
            )
            onReactNotification != null -> Notification.React(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                emoji = onReactNotification.emoji,
                customEmoji = onReactNotification.customEmoji?.let {
                    CustomEmoji(
                        id = it.id,
                        name = it.name,
                        imageUrl = it.imageUrl
                    )
                },
                post = onReactNotification.post?.postFields?.toPost()
            )
            else -> null
        }
    }

    private fun GqlPostVisibility?.toPostVisibility(): PostVisibility {
        return when (this) {
            GqlPostVisibility.PUBLIC -> PostVisibility.PUBLIC
            GqlPostVisibility.UNLISTED -> PostVisibility.UNLISTED
            GqlPostVisibility.FOLLOWERS -> PostVisibility.FOLLOWERS
            GqlPostVisibility.DIRECT -> PostVisibility.DIRECT
            GqlPostVisibility.NONE -> PostVisibility.NONE
            else -> PostVisibility.PUBLIC
        }
    }
}
