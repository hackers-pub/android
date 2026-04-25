package pub.hackers.android.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pub.hackers.android.domain.model.*
import pub.hackers.android.graphql.ArticleDraftQuery
import pub.hackers.android.graphql.ArticleDraftsQuery
import pub.hackers.android.graphql.ArticleForEditQuery
import pub.hackers.android.graphql.ActorArticlesQuery
import pub.hackers.android.graphql.ActorByHandleQuery
import pub.hackers.android.graphql.ActorNotesQuery
import pub.hackers.android.graphql.ActorPostsQuery
import pub.hackers.android.graphql.AddReactionToPostMutation
import pub.hackers.android.graphql.BookmarkPostMutation
import pub.hackers.android.graphql.BookmarksQuery
import pub.hackers.android.graphql.BlockActorMutation
import pub.hackers.android.graphql.CompleteLoginChallengeMutation
import pub.hackers.android.graphql.CreateNoteMutation
import pub.hackers.android.graphql.DeleteArticleDraftMutation
import pub.hackers.android.graphql.DeletePostMutation
import pub.hackers.android.graphql.EditAccountQuery
import pub.hackers.android.graphql.GetPasskeyAuthenticationOptionsMutation
import pub.hackers.android.graphql.GetPasskeyRegistrationOptionsMutation
import pub.hackers.android.graphql.FollowActorMutation
import pub.hackers.android.graphql.LocalTimelineQuery
import pub.hackers.android.graphql.LoginByPasskeyMutation
import pub.hackers.android.graphql.LoginByUsernameMutation
import pub.hackers.android.graphql.MarkNotificationsAsReadMutation
import pub.hackers.android.graphql.NotificationsQuery
import pub.hackers.android.graphql.PersonalTimelineQuery
import pub.hackers.android.graphql.PostQuotesQuery
import pub.hackers.android.graphql.PostRepliesQuery
import pub.hackers.android.graphql.PostSharesQuery
import pub.hackers.android.graphql.PostByUrlQuery
import pub.hackers.android.graphql.PostDetailQuery
import pub.hackers.android.graphql.PublishArticleDraftMutation
import pub.hackers.android.graphql.PublicTimelineQuery
import pub.hackers.android.graphql.RecommendedActorsQuery
import pub.hackers.android.graphql.RemoveFollowerMutation
import pub.hackers.android.graphql.RemoveReactionFromPostMutation
import pub.hackers.android.graphql.RevokePasskeyMutation
import pub.hackers.android.graphql.RevokeSessionMutation
import pub.hackers.android.graphql.SaveArticleDraftMutation
import pub.hackers.android.graphql.ViewerPasskeysQuery
import pub.hackers.android.graphql.VerifyPasskeyRegistrationMutation
import pub.hackers.android.graphql.SearchActorsByHandleQuery
import pub.hackers.android.graphql.SearchObjectQuery
import pub.hackers.android.graphql.SearchPostQuery
import pub.hackers.android.graphql.SharePostMutation
import pub.hackers.android.graphql.UnblockActorMutation
import pub.hackers.android.graphql.UnfollowActorMutation
import pub.hackers.android.graphql.UnbookmarkPostMutation
import pub.hackers.android.graphql.UnsharePostMutation
import pub.hackers.android.graphql.UpdateAccountMutation
import pub.hackers.android.graphql.UpdateArticleMutation
import pub.hackers.android.graphql.ViewerQuery
import pub.hackers.android.graphql.type.AccountLinkInput
import pub.hackers.android.graphql.type.UpdateAccountInput
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
                withContext(Dispatchers.Default) {
                    Result.success(
                        TimelineResult(
                            posts = data?.edges?.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            }?.distinctBy { it.id } ?: emptyList(),
                            hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                            endCursor = data?.pageInfo?.endCursor
                        )
                    )
                }
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
                withContext(Dispatchers.Default) {
                    Result.success(
                        TimelineResult(
                            posts = data?.edges?.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            }?.distinctBy { it.id } ?: emptyList(),
                            hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                            endCursor = data?.pageInfo?.endCursor
                        )
                    )
                }
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
                withContext(Dispatchers.Default) {
                    Result.success(
                        TimelineResult(
                            posts = data?.edges?.map { edge ->
                                edge.node.postFields.toPost(
                                    sharedPost = edge.node.sharedPost?.sharedPostFields?.toPost(),
                                    replyTarget = edge.node.replyTarget?.postFields?.toPost(),
                                    visibility = edge.node.visibility.toPostVisibility(),
                                    lastSharer = edge.lastSharer?.actorFields?.toActor(),
                                    sharersCount = edge.sharersCount
                                )
                            } ?: emptyList(),
                            hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                            endCursor = data?.pageInfo?.endCursor
                        )
                    )
                }
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
                withContext(Dispatchers.Default) {
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
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationsAsRead(): Result<Unit> {
        return try {
            val response = apolloClient.mutation(MarkNotificationsAsReadMutation()).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bookmarkPost(postId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(BookmarkPostMutation(postId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else if (response.data?.bookmarkPost?.onBookmarkPostPayload != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to bookmark"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unbookmarkPost(postId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(UnbookmarkPostMutation(postId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else if (response.data?.unbookmarkPost?.onUnbookmarkPostPayload != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove bookmark"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPosts(
        query: String,
        languages: List<String>
    ): Result<List<Post>> {
        return try {
            val response = apolloClient.query(
                SearchPostQuery(
                    query = query,
                    languages = Optional.present(languages)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val posts = response.data?.searchPost?.edges?.map { edge ->
                    edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                } ?: emptyList()
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookmarks(
        after: String? = null,
        postType: pub.hackers.android.graphql.type.PostType? = null,
    ): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(
                BookmarksQuery(
                    after = Optional.presentIfNotNull(after),
                    postType = Optional.presentIfNotNull(postType)
                )
            ).fetchPolicy(FetchPolicy.NetworkOnly).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data?.bookmarks
                withContext(Dispatchers.Default) {
                    Result.success(
                        TimelineResult(
                            posts = data?.edges?.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            } ?: emptyList(),
                            hasNextPage = data?.pageInfo?.hasNextPage ?: false,
                            endCursor = data?.pageInfo?.endCursor
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostDetail(id: String, repliesAfter: String? = null): Result<PostDetailResult> {
        return try {
            val response = apolloClient.query(
                PostDetailQuery(id, Optional.presentIfNotNull(repliesAfter))
            ).fetchPolicy(FetchPolicy.NetworkOnly).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                withContext(Dispatchers.Default) {
                    val node = response.data?.node?.onPost
                        ?: return@withContext Result.failure(Exception("Post not found"))

                    val post = node.postFields.toPost(
                        sharedPost = node.sharedPost?.sharedPostFields?.toPost(),
                        replyTarget = node.replyTarget?.postFields?.toPost(),
                        visibility = node.visibility.toPostVisibility()
                    )

                    val toc = response.data?.node?.onArticle?.contents
                        ?.firstOrNull()
                        ?.toc
                        ?.let { parseTocJson(it) }
                        ?: emptyList()

                    val reactionGroups = node.reactionGroups.mapNotNull { group ->
                        when {
                            group.onEmojiReactionGroup != null -> ReactionGroup(
                                emoji = group.onEmojiReactionGroup.emoji,
                                customEmoji = null,
                                count = group.onEmojiReactionGroup.reactors.totalCount,
                                reactors = group.onEmojiReactionGroup.reactors.edges.map {
                                    it.node.actorFields.toActor()
                                },
                                viewerHasReacted = group.onEmojiReactionGroup.reactors.viewerHasReacted
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
                                },
                                viewerHasReacted = group.onCustomEmojiReactionGroup.reactors.viewerHasReacted
                            )
                            else -> null
                        }
                    }

                    val replies = node.replies.edges.map { edge ->
                        edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                    }

                    Result.success(
                        PostDetailResult(
                            post = post,
                            reactionGroups = reactionGroups,
                            replies = replies,
                            hasMoreReplies = node.replies.pageInfo.hasNextPage,
                            repliesEndCursor = node.replies.pageInfo.endCursor,
                            toc = toc,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolvePostIdByUrl(url: String): Result<String> {
        return try {
            val response = apolloClient.query(
                PostByUrlQuery(url)
            ).fetchPolicy(FetchPolicy.NetworkOnly).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val postId = response.data?.postByUrl?.id
                    ?: return Result.failure(Exception("Post not found"))
                Result.success(postId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(handle: String, refresh: Boolean = false): Result<ProfileResult> {
        return try {
            val response = apolloClient.query(
                ActorByHandleQuery(handle)
            ).apply { if (refresh) fetchPolicy(FetchPolicy.NetworkOnly) }.execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                withContext(Dispatchers.Default) {
                    val actor = response.data?.actorByHandle
                        ?: return@withContext Result.failure(Exception("Actor not found"))

                    Result.success(
                        ProfileResult(
                            actor = Actor(
                                id = actor.id,
                                name = actor.name?.toString(),
                                handle = actor.handle,
                                avatarUrl = actor.avatarUrl.toString()
                            ),
                            bio = actor.bio?.toString(),
                            fields = actor.fields.map { field ->
                                ActorField(
                                    name = field.name,
                                    value = field.value.toString()
                                )
                            },
                            accountLinks = actor.account?.links?.map { link ->
                                AccountLink(
                                    name = link.name,
                                    handle = link.handle,
                                    icon = link.icon.name.lowercase(),
                                    url = link.url.toString(),
                                    verified = link.verified?.toString()
                                )
                            } ?: emptyList(),
                            isViewer = actor.isViewer,
                            viewerFollows = actor.viewerFollows,
                            followsViewer = actor.followsViewer,
                            viewerBlocks = actor.viewerBlocks
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Paginated replies for a post. Separate from [getPostDetail] so that
     * scrolling the reply list never re-fetches the main post body.
     *
     * The mapping block runs in `Dispatchers.Default` because the caller is
     * typically `viewModelScope.launch` (Main.immediate) and the `.mapNotNull`
     * over nested post graphs is CPU-bound. The Apollo `.execute()` call
     * itself is already dispatched off the main thread by OkHttp/Apollo.
     */
    suspend fun getPostReplies(postId: String, after: String? = null): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(
                PostRepliesQuery(postId, Optional.presentIfNotNull(after))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val replies = response.data?.node?.onPost?.replies
                    ?: return Result.failure(Exception("Post not found"))

                withContext(Dispatchers.Default) {
                    Result.success(
                        TimelineResult(
                            // Defensive dedup by outer id: server occasionally returns
                            // duplicate edges across pages (PR #83). Paging layer adds
                            // distinctByEffectiveId (sharedPost.id ?: id) in PostOverlay.kt.
                            posts = replies.edges.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            }.distinctBy { it.id },
                            hasNextPage = replies.pageInfo.hasNextPage,
                            endCursor = replies.pageInfo.endCursor
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lightweight actor posts query for the POSTS tab on profile. Mirrors
     * [getActorNotes] / [getActorArticles]; avoids re-fetching the actor
     * header on every paginated page (unlike [getProfile]).
     */
    suspend fun getActorPosts(handle: String, after: String? = null): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(
                ActorPostsQuery(handle, Optional.presentIfNotNull(after))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val posts = response.data?.actorByHandle?.posts
                    ?: return Result.failure(Exception("Actor not found"))

                withContext(Dispatchers.Default) {
                    Result.success(
                        TimelineResult(
                            // Defensive dedup by outer id: server occasionally returns
                            // duplicate edges across pages (PR #83). Paging layer adds
                            // distinctByEffectiveId (sharedPost.id ?: id) in PostOverlay.kt.
                            posts = posts.edges.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            }.distinctBy { it.id },
                            hasNextPage = posts.pageInfo.hasNextPage,
                            endCursor = posts.pageInfo.endCursor
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActorArticles(handle: String, after: String? = null): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(
                ActorArticlesQuery(handle, Optional.presentIfNotNull(after))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                withContext(Dispatchers.Default) {
                    val articles = response.data?.actorByHandle?.articles
                        ?: return@withContext Result.failure(Exception("Actor not found"))

                    Result.success(
                        TimelineResult(
                            posts = articles.edges.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            },
                            hasNextPage = articles.pageInfo.hasNextPage,
                            endCursor = articles.pageInfo.endCursor
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActorNotes(handle: String, after: String? = null): Result<TimelineResult> {
        return try {
            val response = apolloClient.query(
                ActorNotesQuery(handle, Optional.presentIfNotNull(after))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                withContext(Dispatchers.Default) {
                    val notes = response.data?.actorByHandle?.notes
                        ?: return@withContext Result.failure(Exception("Actor not found"))

                    Result.success(
                        TimelineResult(
                            posts = notes.edges.map { edge ->
                                edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                            },
                            hasNextPage = notes.pageInfo.hasNextPage,
                            endCursor = notes.pageInfo.endCursor
                        )
                    )
                }
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

    suspend fun getEditableAccount(): Result<EditableAccount> {
        return try {
            val response = apolloClient.query(EditAccountQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val viewer = response.data?.viewer
                    ?: return Result.failure(Exception("Not signed in"))
                Result.success(
                    EditableAccount(
                        id = viewer.id,
                        name = viewer.name,
                        bio = viewer.bio.toString(),
                        avatarUrl = viewer.avatarUrl.toString(),
                        handle = viewer.handle,
                        links = viewer.links.map { link ->
                            EditableAccountLink(name = link.name, url = link.url.toString())
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAccount(
        id: String,
        name: String,
        bio: String,
        avatarUrl: String?,
        links: List<EditableAccountLink>,
    ): Result<Unit> {
        return try {
            val response = apolloClient.mutation(
                UpdateAccountMutation(
                    input = UpdateAccountInput(
                        id = id,
                        name = Optional.present(name),
                        bio = Optional.present(bio),
                        avatarUrl = if (avatarUrl != null) Optional.present(avatarUrl) else Optional.Absent,
                        links = Optional.present(
                            links.map { AccountLinkInput(name = it.name, url = it.url) }
                        ),
                    )
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else if (response.data?.updateAccount?.account == null) {
                Result.failure(Exception("Update failed"))
            } else {
                Result.success(Unit)
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
                    verifyUrl = "https://hackers.pub/applink/sign/in/{token}?code={code}"
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

    suspend fun getPasskeyAuthenticationOptions(sessionId: String): Result<String> {
        return try {
            val response = apolloClient.mutation(
                GetPasskeyAuthenticationOptionsMutation(sessionId = sessionId)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val options = response.data?.getPasskeyAuthenticationOptions
                    ?: return Result.failure(Exception("Failed to get passkey options"))
                Result.success(toJsonString(options))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginByPasskey(sessionId: String, authenticationResponse: Any): Result<Session> {
        return try {
            val response = apolloClient.mutation(
                LoginByPasskeyMutation(
                    sessionId = sessionId,
                    authenticationResponse = authenticationResponse,
                    platform = Optional.present("android")
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val session = response.data?.loginByPasskey
                    ?: return Result.failure(Exception("Passkey authentication failed"))

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

    suspend fun getPasskeyRegistrationOptions(accountId: String): Result<String> {
        return try {
            val response = apolloClient.mutation(
                GetPasskeyRegistrationOptionsMutation(accountId = accountId)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val options = response.data?.getPasskeyRegistrationOptions
                    ?: return Result.failure(Exception("Failed to get registration options"))
                Result.success(toJsonString(options))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPasskeyRegistration(
        accountId: String,
        name: String,
        registrationResponse: Any
    ): Result<PasskeyRegistrationResult> {
        return try {
            val response = apolloClient.mutation(
                VerifyPasskeyRegistrationMutation(
                    accountId = accountId,
                    name = name,
                    registrationResponse = registrationResponse,
                    platform = Optional.present("android")
                )
            ).execute()

            if (response.hasErrors()) {
                val errors = response.errors?.map { "${it.message} path=${it.path}" }
                android.util.Log.e("PasskeyAuth", "verifyPasskeyRegistration errors: $errors")
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.verifyPasskeyRegistration
                    ?: return Result.failure(Exception("Registration verification failed"))

                Result.success(
                    PasskeyRegistrationResult(
                        verified = result.verified,
                        passkey = result.passkey?.let {
                            Passkey(
                                id = it.id,
                                name = it.name,
                                created = it.created.toString(),
                                lastUsed = it.lastUsed?.toString()
                            )
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokePasskey(passkeyId: String): Result<String?> {
        return try {
            val response = apolloClient.mutation(
                RevokePasskeyMutation(passkeyId = passkeyId)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                Result.success(response.data?.revokePasskey)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class PasskeysResult(
        val accountId: String,
        val passkeys: List<Passkey>
    )

    suspend fun getPasskeys(): Result<PasskeysResult> {
        return try {
            val response = apolloClient.query(ViewerPasskeysQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val viewer = response.data?.viewer
                    ?: return Result.failure(Exception("Not authenticated"))

                val passkeys = viewer.passkeys.edges.map { edge ->
                    Passkey(
                        id = edge.node.id,
                        name = edge.node.name,
                        created = edge.node.created.toString(),
                        lastUsed = edge.node.lastUsed?.toString()
                    )
                }

                Result.success(PasskeysResult(accountId = viewer.id, passkeys = passkeys))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNote(
        content: String,
        language: String = "en",
        visibility: PostVisibility = PostVisibility.PUBLIC,
        replyTargetId: String? = null,
        quotedPostId: String? = null
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
                    replyTargetId = Optional.presentIfNotNull(replyTargetId),
                    quotedPostId = Optional.presentIfNotNull(quotedPostId)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.createNote
                when {
                    result?.onCreateNotePayload != null -> {
                        Result.success(result.onCreateNotePayload.note.postFields.toPost())
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

    suspend fun getRecommendedActors(limit: Int = 10): Result<List<Actor>> {
        return try {
            val response = apolloClient.query(
                RecommendedActorsQuery(limit = Optional.present(limit))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val actors = response.data?.recommendedActors?.map { actor ->
                    Actor(
                        id = actor.id,
                        name = actor.name?.toString(),
                        handle = actor.handle,
                        avatarUrl = actor.avatarUrl.toString(),
                        bio = actor.bio?.toString()
                    )
                } ?: emptyList()
                Result.success(actors)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun followActor(actorId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(FollowActorMutation(actorId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.followActor
                when {
                    result?.onFollowActorPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowActor(actorId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(UnfollowActorMutation(actorId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.unfollowActor
                when {
                    result?.onUnfollowActorPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockActor(actorId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(BlockActorMutation(actorId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.blockActor
                when {
                    result?.onBlockActorPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockActor(actorId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(UnblockActorMutation(actorId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.unblockActor
                when {
                    result?.onUnblockActorPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFollower(actorId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(RemoveFollowerMutation(actorId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.removeFollower
                when {
                    result?.onRemoveFollowerPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchObject(query: String): Result<String?> {
        return try {
            val response = apolloClient.query(SearchObjectQuery(query)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val url = response.data?.searchObject?.onSearchedObject?.url
                Result.success(url)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostShares(postId: String, after: String? = null): Result<SharesResult> {
        return try {
            val response = apolloClient.query(
                PostSharesQuery(postId, Optional.presentIfNotNull(after))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val shares = response.data?.node?.onPost?.shares
                    ?: return Result.failure(Exception("Post not found"))

                Result.success(
                    SharesResult(
                        actors = shares.edges.map { edge ->
                            edge.node.actor.actorFields.toActor()
                        },
                        hasNextPage = shares.pageInfo.hasNextPage,
                        endCursor = shares.pageInfo.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostQuotes(postId: String, after: String? = null): Result<QuotesResult> {
        return try {
            val response = apolloClient.query(
                PostQuotesQuery(postId, Optional.presentIfNotNull(after))
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val quotes = response.data?.node?.onPost?.quotes
                    ?: return Result.failure(Exception("Post not found"))

                Result.success(
                    QuotesResult(
                        posts = quotes.edges.map { edge ->
                            edge.node.postFields.toPost(edge.node.sharedPost?.sharedPostFields?.toPost())
                        },
                        hasNextPage = quotes.pageInfo.hasNextPage,
                        endCursor = quotes.pageInfo.endCursor
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReactionToPost(postId: String, emoji: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(
                AddReactionToPostMutation(postId = postId, emoji = emoji)
            ).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.addReactionToPost
                when {
                    result?.onAddReactionToPostPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeReactionFromPost(postId: String, emoji: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(
                RemoveReactionFromPostMutation(postId = postId, emoji = emoji)
            ).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.removeReactionFromPost
                when {
                    result?.onRemoveReactionFromPostPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(DeletePostMutation(postId)).execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.deletePost
                when {
                    result?.onDeletePostPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    result?.onSharedPostDeletionNotAllowedError != null ->
                        Result.failure(Exception("Cannot delete a shared post"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveArticleDraft(
        title: String,
        content: String,
        tags: List<String>,
        id: String? = null
    ): Result<ArticleDraft> {
        return try {
            val response = apolloClient.mutation(
                SaveArticleDraftMutation(
                    title = title,
                    content = content,
                    tags = tags,
                    id = Optional.presentIfNotNull(id)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.saveArticleDraft
                when {
                    result?.onSaveArticleDraftPayload != null -> {
                        val draft = result.onSaveArticleDraftPayload.draft
                        Result.success(
                            ArticleDraft(
                                id = draft.id,
                                title = draft.title,
                                content = draft.content.toString(),
                                contentHtml = draft.contentHtml.toString(),
                                tags = draft.tags,
                                created = Instant.parse(draft.created.toString()),
                                updated = Instant.parse(draft.updated.toString())
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

    suspend fun deleteArticleDraft(id: String): Result<Unit> {
        return try {
            val response = apolloClient.mutation(DeleteArticleDraftMutation(id)).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.deleteArticleDraft
                when {
                    result?.onDeleteArticleDraftPayload != null -> Result.success(Unit)
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishArticleDraft(
        id: String,
        slug: String,
        language: String,
        allowLlmTranslation: Boolean = true
    ): Result<PublishedArticle> {
        return try {
            val response = apolloClient.mutation(
                PublishArticleDraftMutation(
                    id = id,
                    slug = slug,
                    language = language,
                    allowLlmTranslation = Optional.present(allowLlmTranslation)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.publishArticleDraft
                when {
                    result?.onPublishArticleDraftPayload != null -> {
                        val article = result.onPublishArticleDraftPayload.article
                        Result.success(
                            PublishedArticle(
                                id = article.id,
                                name = article.name,
                                url = article.url?.toString()
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

    suspend fun getArticleDrafts(): Result<List<ArticleDraft>> {
        return try {
            val response = apolloClient.query(ArticleDraftsQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val drafts = response.data?.viewer?.articleDrafts?.edges?.map { edge ->
                    val node = edge.node
                    ArticleDraft(
                        id = node.id,
                        title = node.title,
                        content = node.content.toString(),
                        contentHtml = "",
                        tags = node.tags,
                        created = Instant.parse(node.created.toString()),
                        updated = Instant.parse(node.updated.toString())
                    )
                } ?: emptyList()
                Result.success(drafts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArticleDraft(id: String): Result<ArticleDraft> {
        return try {
            val response = apolloClient.query(ArticleDraftQuery(id))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val draft = response.data?.articleDraft
                    ?: return Result.failure(Exception("Draft not found"))
                Result.success(
                    ArticleDraft(
                        id = draft.id,
                        title = draft.title,
                        content = draft.content.toString(),
                        contentHtml = draft.contentHtml.toString(),
                        tags = draft.tags,
                        created = Instant.parse(draft.created.toString()),
                        updated = Instant.parse(draft.updated.toString())
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEditableArticle(articleId: String): Result<EditableArticle> {
        return try {
            val response = apolloClient.query(ArticleForEditQuery(articleId))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val article = response.data?.node?.onArticle
                    ?: return Result.failure(Exception("Article not found"))
                val original = article.contents.firstOrNull { it.originalLanguage == null }
                    ?: article.contents.firstOrNull()
                    ?: return Result.failure(Exception("Article content not found"))
                Result.success(
                    EditableArticle(
                        id = article.id,
                        title = original.title,
                        content = original.rawContent.toString(),
                        tags = article.tags,
                        language = original.language.toString(),
                        allowLlmTranslation = article.allowLlmTranslation
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateArticle(
        articleId: String,
        title: String,
        content: String,
        tags: List<String>,
        language: String,
        allowLlmTranslation: Boolean
    ): Result<PublishedArticle> {
        return try {
            val response = apolloClient.mutation(
                UpdateArticleMutation(
                    articleId = articleId,
                    title = Optional.present(title),
                    content = Optional.present(content),
                    tags = Optional.present(tags),
                    language = Optional.present(language),
                    allowLlmTranslation = Optional.present(allowLlmTranslation)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val result = response.data?.updateArticle
                when {
                    result?.onUpdateArticlePayload != null -> {
                        val article = result.onUpdateArticlePayload.article
                        Result.success(
                            PublishedArticle(
                                id = article.id,
                                name = article.name,
                                url = article.url?.toString()
                            )
                        )
                    }
                    result?.onInvalidInputError != null ->
                        Result.failure(Exception("Invalid input: ${result.onInvalidInputError.inputPath}"))
                    result?.onNotAuthenticatedError != null ->
                        Result.failure(Exception("Not authenticated"))
                    else -> Result.failure(Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions to convert GraphQL fragment types to domain models
    private fun PostFields.toPost(
        sharedPost: Post? = null,
        replyTarget: Post? = null,
        visibility: PostVisibility = PostVisibility.PUBLIC,
        lastSharer: Actor? = null,
        sharersCount: Int = 0
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
            iri = iri.toString(),
            viewerHasShared = viewerHasShared,
            viewerHasBookmarked = viewerHasBookmarked,
            actor = actor.actorFields.toActor(),
            media = media.map { it.mediaFields.toMedia() },
            link = link?.let { l ->
                PostLink(
                    title = l.title,
                    description = l.description,
                    url = l.url.toString(),
                    siteName = l.siteName,
                    author = l.author,
                    image = l.image?.let { img ->
                        PostLinkImage(
                            url = img.url.toString(),
                            alt = img.alt,
                            width = img.width,
                            height = img.height
                        )
                    },
                    creator = l.creator?.actorFields?.toActor()
                )
            },
            engagementStats = engagementStats.engagementStatsFields.toEngagementStats(),
            mentions = mentions.edges.map { it.node.handle },
            lastSharer = lastSharer,
            sharersCount = sharersCount,
            sharedPost = sharedPost,
            replyTarget = replyTarget,
            quotedPost = quotedPost?.sharedPostFields?.toPost(),
            visibility = visibility,
            reactionGroups = reactionGroups.mapNotNull { group ->
                when {
                    group.onEmojiReactionGroup != null -> ReactionGroup(
                        emoji = group.onEmojiReactionGroup.emoji,
                        customEmoji = null,
                        count = group.onEmojiReactionGroup.reactors.totalCount,
                        reactors = emptyList(),
                        viewerHasReacted = group.onEmojiReactionGroup.reactors.viewerHasReacted
                    )
                    group.onCustomEmojiReactionGroup != null -> ReactionGroup(
                        emoji = null,
                        customEmoji = CustomEmoji(
                            id = group.onCustomEmojiReactionGroup.customEmoji.id,
                            name = group.onCustomEmojiReactionGroup.customEmoji.name,
                            imageUrl = group.onCustomEmojiReactionGroup.customEmoji.imageUrl
                        ),
                        count = group.onCustomEmojiReactionGroup.reactors.totalCount,
                        reactors = emptyList(),
                        viewerHasReacted = group.onCustomEmojiReactionGroup.reactors.viewerHasReacted
                    )
                    else -> null
                }
            }
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
            iri = iri.toString(),
            viewerHasShared = viewerHasShared,
            viewerHasBookmarked = viewerHasBookmarked,
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
            width = width,
            mediaType = "$type"
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
                post = onMentionNotification.post?.let { NotificationPost(id = it.id, content = it.content.toString()) }
            )
            onReplyNotification != null -> Notification.Reply(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onReplyNotification.post?.let { NotificationPost(id = it.id, content = it.content.toString()) }
            )
            onQuoteNotification != null -> Notification.Quote(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onQuoteNotification.post?.let { NotificationPost(id = it.id, content = it.content.toString()) }
            )
            onShareNotification != null -> Notification.Share(
                id = id,
                uuid = uuid.toString(),
                created = created,
                actors = actors,
                post = onShareNotification.post?.let { NotificationPost(id = it.id, content = it.content.toString()) }
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
                post = onReactNotification.post?.let { NotificationPost(id = it.id, content = it.content.toString()) }
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

    private fun toJsonString(obj: Any?): String {
        return when (obj) {
            is Map<*, *> -> {
                val jsonObj = org.json.JSONObject()
                obj.forEach { (k, v) -> jsonObj.put(k.toString(), toJsonValue(v)) }
                jsonObj.toString()
            }
            is List<*> -> {
                val jsonArr = org.json.JSONArray()
                obj.forEach { jsonArr.put(toJsonValue(it)) }
                jsonArr.toString()
            }
            else -> obj.toString()
        }
    }

    private fun toJsonValue(value: Any?): Any? {
        return when (value) {
            null -> org.json.JSONObject.NULL
            is Map<*, *> -> {
                val jsonObj = org.json.JSONObject()
                value.forEach { (k, v) -> jsonObj.put(k.toString(), toJsonValue(v)) }
                jsonObj
            }
            is List<*> -> {
                val jsonArr = org.json.JSONArray()
                value.forEach { jsonArr.put(toJsonValue(it)) }
                jsonArr
            }
            else -> value
        }
    }

}
