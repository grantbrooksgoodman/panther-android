//
//  ConversationCellViewData.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.filteringSystemMessages
import us.neotechnica.panther.modules.content.user.extensions.isConsentAcknowledgementMessage
import us.neotechnica.panther.modules.content.user.extensions.isConsentMessage
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.textContains
import us.neotechnica.panther.modules.content.user.extensions.withMessagesOffsetFromCurrentUserAdditionDate
import us.neotechnica.panther.modules.content.user.extensions.withMessagesSortedByAscendingSentDate
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.extensions.formattedShortString
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The display data for a conversation cell.
 *
 * [ConversationCellViewData] derives everything a conversation cell
 * renders – title, subtitle, date, thumbnail, and unread state – from
 * a conversation. Derived values are cached in memory per
 * conversation and search query.
 *
 * @property titleLabelText The text the title label displays.
 * @property subtitleLabelText The text the subtitle label displays.
 * @property dateLabelText The text the date label displays.
 * @property isShowingUnreadIndicator A Boolean value that indicates
 *   whether the unread indicator is shown.
 * @property otherUser The other user in a one-to-one conversation, or
 *   `null` for group conversations.
 * @property thumbnailImage The image the avatar displays, or `null`
 *   to show a placeholder symbol.
 * @property isAwaitingMessageHydration A Boolean value that indicates
 *   whether the conversation's messages have not yet been hydrated
 *   into the session store.
 */
data class ConversationCellViewData(
    val titleLabelText: String,
    val subtitleLabelText: String,
    val dateLabelText: String,
    val isShowingUnreadIndicator: Boolean,
    val otherUser: User?,
    val thumbnailImage: Bitmap?,
    val isAwaitingMessageHydration: Boolean = false,
) {
    // MARK: - Init

    /**
     * Creates view data whose only populated value is the given other
     * user.
     *
     * @param user The other user in the conversation.
     */
    constructor(user: User) : this(
        titleLabelText = empty.titleLabelText,
        subtitleLabelText = empty.subtitleLabelText,
        dateLabelText = empty.dateLabelText,
        isShowingUnreadIndicator = empty.isShowingUnreadIndicator,
        otherUser = user,
        thumbnailImage = empty.thumbnailImage,
        isAwaitingMessageHydration = empty.isAwaitingMessageHydration,
    )

    // MARK: - Companion

    companion object {
        /** An empty placeholder value. */
        val empty =
            ConversationCellViewData(
                titleLabelText = "",
                subtitleLabelText = "",
                dateLabelText = "",
                isShowingUnreadIndicator = false,
                otherUser = null,
                thumbnailImage = null,
            )

        /**
         * Derives view data for the given conversation.
         *
         * The title comes from the conversation's name, the
         * participants' contact names, or a participant's phone
         * number. The subtitle and date describe the latest message
         * or activity – with an active search query, the most recent
         * matching message instead. The unread indicator reflects
         * whether the current user has read the latest message from
         * other users.
         *
         * Fully derived values are cached in memory per conversation
         * and search query; data derived while messages are still
         * hydrating is never cached. Returns `null` if the
         * conversation's users cannot be resolved.
         *
         * @param conversation The conversation for which to derive
         *   view data.
         * @param searchQuery The active search query, or `null` when
         *   none is active.
         * @param useCachedValue A Boolean value that indicates whether
         *   a cached value may be returned.
         *
         * @return The derived view data, or `null` if the
         *   conversation's users cannot be resolved.
         */
        @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
        operator fun invoke(
            conversation: Conversation,
            searchQuery: String? = null,
            useCachedValue: Boolean = true,
        ): ConversationCellViewData? {
            val sessionStore = DependencyValues.current.clientSession.store

            val cacheQuery = if (searchQuery.isNullOrBlank()) BANG_QUALIFIED_EMPTY else searchQuery
            if (useCachedValue && !conversation.isMock) {
                ConversationCellViewDataCache.cachedValue(cacheQuery, conversation.id)?.let { return it }
            }

            val sortedConversation = conversation.withMessagesSortedByAscendingSentDate
            val users = sortedConversation.users ?: return null
            val lastUser = users.lastOrNull() ?: return null

            var titleLabelText: String
            var subtitleLabelText = ""
            var dateLabelText = ""
            var thumbnailImage: Bitmap? = null
            var isShowingUnreadIndicator = false
            var otherUser: User? = null

            // Set title label text

            val contactPair =
                users
                    .mapNotNull { it.contactPair }
                    .sortedBy { it.contact.fullName }
                    .firstOrNull()

            if (!sortedConversation.metadata.name.isBangQualifiedEmpty) {
                titleLabelText = sortedConversation.metadata.name
            } else if (contactPair != null) {
                titleLabelText = contactPair.contact.fullName
                val image = contactPair.contact.image
                if (image != null && users.size == 1) thumbnailImage = image
            } else {
                titleLabelText = lastUser.phoneNumber.formattedString()
            }

            if (sortedConversation.metadata.name.isBangQualifiedEmpty) {
                if (users.size > 1) {
                    titleLabelText += " + ${users.size - 1}"
                } else {
                    otherUser = users.first()
                }
            }

            // Set thumbnail image

            if (users.size > 1) {
                thumbnailImage = sortedConversation.metadata.image ?: thumbnailImage
            }

            // Set date & subtitle label text

            val messages =
                sortedConversation
                    .withMessagesOffsetFromCurrentUserAdditionDate
                    .messages
                    ?.filteringSystemMessages

            // Distinguishes "no messages exist" from "messages not yet
            // hydrated into the session store".
            val hasUnresolvedMessages =
                sortedConversation.messageIDs
                    .filter { it.startsWith("-") }
                    .any { sessionStore.messages[it] == null }

            var messageMatchingSearchQuery: Message? = null
            if (!searchQuery.isNullOrBlank()) {
                messageMatchingSearchQuery = messages?.lastOrNull { it.textContains(searchQuery) }
            }

            val lastMessage = messageMatchingSearchQuery ?: messages?.lastOrNull()
            val latestActivity =
                sortedConversation.activities
                    ?.filter { it != Activity.empty }
                    ?.maxByOrNull { it.date }

            if (latestActivity != null &&
                messageMatchingSearchQuery == null &&
                latestActivity.date > (lastMessage?.sentDate ?: Date(Long.MIN_VALUE)) &&
                (lastMessage != null || !hasUnresolvedMessages)
            ) {
                // Activities are the canonical source for system messages, whose
                // sent dates count equally toward the latest message resolution.
                dateLabelText = dateLabelText(latestActivity.date)
                subtitleLabelText = latestActivity.description.sanitized
            } else if (lastMessage != null) {
                dateLabelText = dateLabelText(lastMessage.sentDate)
                subtitleLabelText = subtitleLabelText(lastMessage)
            } else if (!hasUnresolvedMessages) {
                // The fallback applies only to fully hydrated conversations;
                // provisional builds keep placeholder values instead.
                dateLabelText = Date(0).formattedShortString
                subtitleLabelText = LocalizedStringKey.CannotDisplayMessage.localized()
            }

            // Set unread indicator status

            messages?.lastOrNull { !it.isFromCurrentUser }?.let { lastMessageFromOtherUsers ->
                isShowingUnreadIndicator = lastMessageFromOtherUsers.currentUserReadReceipt == null
            }

            val data =
                ConversationCellViewData(
                    titleLabelText = titleLabelText,
                    subtitleLabelText = subtitleLabelText,
                    dateLabelText = dateLabelText,
                    isShowingUnreadIndicator = isShowingUnreadIndicator,
                    otherUser = otherUser,
                    thumbnailImage = thumbnailImage,
                    isAwaitingMessageHydration = hasUnresolvedMessages,
                )

            // Never cache provisional data; the next build must re-derive
            // it from the hydrated conversation.
            if (hasUnresolvedMessages) return data

            ConversationCellViewDataCache.cache(cacheQuery, conversation.id, data)
            return data
        }

        // MARK: - Auxiliary

        private fun dateLabelText(date: Date): String {
            if (Application.isInStagingMode && Calendar.getInstance().isDateInToday(date)) {
                return stagingModeDateFormatter.format(date)
            }

            return date.formattedShortString
        }

        private fun subtitleLabelText(lastMessage: Message): String {
            if (lastMessage.audioComponent != null) {
                return "🔊 ${LocalizedStringKey.AudioMessage.localized()}"
            } else if (lastMessage.documentComponent != null) {
                return "📄 ${LocalizedStringKey.Document.localized()}"
            } else if (lastMessage.imageComponent != null) {
                return "🏞️ ${LocalizedStringKey.Image.localized()}"
            } else if (lastMessage.videoComponent != null) {
                return "🎥 ${LocalizedStringKey.Video.localized()}"
            } else if (lastMessage.richContent?.mediaComponent != null) {
                return "📎 ${LocalizedStringKey.Attachment.localized()}"
            }

            val translation = lastMessage.translation ?: return ""
            val consentAcknowledgementMessage = LocalizedStringKey.MessageRecipientConsentAcknowledgementMessage.localized()
            val consentRequestMessage = LocalizedStringKey.MessageRecipientConsentRequestMessage.localized()
            val isLastMessageFromCurrentUser = lastMessage.isFromCurrentUser

            return if (lastMessage.isConsentMessage) {
                val consentMessage =
                    if (lastMessage.isConsentAcknowledgementMessage) consentAcknowledgementMessage else consentRequestMessage
                consentMessage.sanitized.trim()
            } else if (isLastMessageFromCurrentUser) {
                translation.input.value.sanitized
            } else {
                translation.output.sanitized
            }
        }

        private fun Calendar.isDateInToday(date: Date): Boolean {
            val other = Calendar.getInstance().apply { time = date }
            return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
        }

        private val stagingModeDateFormatter: SimpleDateFormat
            get() = SimpleDateFormat(STAGING_MODE_DATE_FORMAT, Locale.US)

        private const val STAGING_MODE_DATE_FORMAT = "h:mm a"
    }
}

/**
 * Manages the in-memory conversation cell view data cache, keyed by
 * search query and then conversation identifier.
 */
object ConversationCellViewDataCache {
    // MARK: - Properties

    private val cachedDataByConversationIDForSearchQueries =
        LockIsolated<Map<String, Map<ConversationID, ConversationCellViewData>>?>(null)

    // MARK: - Methods

    /** Removes every cached conversation cell view data value. */
    fun clearCache() {
        cachedDataByConversationIDForSearchQueries.wrappedValue = null
    }

    /**
     * Removes the cached values for the conversation with the given ID
     * key.
     *
     * @param forConversationIDKey The ID key of the conversation whose
     *   values to remove.
     */
    fun removeValues(forConversationIDKey: String) {
        val cache = cachedDataByConversationIDForSearchQueries.wrappedValue ?: return
        cachedDataByConversationIDForSearchQueries.wrappedValue =
            cache.mapValues { (_, dataByID) -> dataByID.filterKeys { it.key != forConversationIDKey } }
    }

    internal fun cachedValue(
        searchQuery: String,
        conversationID: ConversationID,
    ): ConversationCellViewData? = cachedDataByConversationIDForSearchQueries.wrappedValue?.get(searchQuery)?.get(conversationID)

    internal fun cache(
        searchQuery: String,
        conversationID: ConversationID,
        data: ConversationCellViewData,
    ) {
        val cache = (cachedDataByConversationIDForSearchQueries.wrappedValue ?: emptyMap()).toMutableMap()
        val dataByID = (cache[searchQuery] ?: emptyMap()).toMutableMap()
        dataByID[conversationID] = data
        cache[searchQuery] = dataByID
        cachedDataByConversationIDForSearchQueries.wrappedValue = cache
    }
}

// MARK: - Auxiliary

// The decoded metadata image, resolved once per view data build.
private val us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata.image: Bitmap?
    get() {
        val data = imageData ?: return null
        return runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull()
    }
