//
//  ConversationCellViewData.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.isMock
import us.neotechnica.panther.modules.session.entity.extensions.isReadByCurrentUser
import us.neotechnica.panther.modules.session.entity.extensions.messages
import us.neotechnica.panther.modules.session.entity.extensions.resolvedText
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The display data for a single conversation cell: title, message
 * preview, timestamp, and unread state.
 *
 * **Note:** the iOS original also resolves contact names and photos;
 * this Phase 6 port derives the title from the conversation metadata or
 * the other participant's phone number, with an initials avatar.
 * Contact integration is deferred.
 */
data class ConversationCellViewData(
    val title: String,
    val subtitle: String,
    val dateLabelText: String,
    val isShowingUnreadIndicator: Boolean,
    val initials: String,
    val hasContactName: Boolean,
    val isGroup: Boolean,
    val participantCount: Int,
    val otherLanguageCode: String?,
    val otherRegionCode: String?,
    val otherUser: User?,
) {
    companion object {
        /** An empty placeholder used before a cell's data resolves. */
        val empty =
            ConversationCellViewData(
                title = "",
                subtitle = "",
                dateLabelText = "",
                isShowingUnreadIndicator = false,
                initials = "",
                hasContactName = false,
                isGroup = false,
                participantCount = 0,
                otherLanguageCode = null,
                otherRegionCode = null,
                otherUser = null,
            )

        /**
         * Builds the cell data for [conversation], resolving text into
         * [languageCode].
         *
         * When [searchQuery] is non-blank, the preview and date reflect
         * the most recent message matching the query rather than the
         * latest message, mirroring the iOS search behavior.
         */
        suspend fun build(
            conversation: Conversation,
            languageCode: String,
            searchQuery: String = "",
            useCachedValue: Boolean = true,
        ): ConversationCellViewData {
            val cacheQuery = searchQuery.ifBlank { CACHE_QUERY_EMPTY }
            if (useCachedValue && !conversation.isMock) {
                ConversationCellViewDataCache.cachedValue(cacheQuery, conversation.id.key)?.let { return it }
            }

            val title = title(conversation)
            val messages = conversation.messages.orEmpty().sortedBy { it.sentDate.time }
            val matchingMessage =
                searchQuery
                    .takeIf { it.isNotBlank() }
                    ?.let { query -> messages.lastOrNull { it.matchesSearchQuery(query) } }
            val lastMessage = matchingMessage ?: messages.lastOrNull()
            val users = conversation.users.orEmpty()
            val isGroup = conversation.participants.size > 2
            val hasName = title.any { it.isLetter() }
            val lastMessageFromOthers = messages.lastOrNull { !it.isFromCurrentUser }

            val data =
                ConversationCellViewData(
                    title = title,
                    subtitle = subtitle(lastMessage, languageCode),
                    dateLabelText =
                        lastMessage?.sentDate?.let { relativeDateString(it) }
                            ?: relativeDateString(conversation.metadata.lastModifiedDate),
                    isShowingUnreadIndicator =
                        lastMessageFromOthers != null && !lastMessageFromOthers.isReadByCurrentUser,
                    initials = if (hasName) initials(title) else "",
                    hasContactName = hasName,
                    isGroup = isGroup,
                    participantCount = users.size,
                    otherLanguageCode = if (!isGroup) users.firstOrNull()?.languageCode else null,
                    otherRegionCode = if (!isGroup) users.firstOrNull()?.phoneNumber?.regionCode else null,
                    otherUser = if (!isGroup) users.firstOrNull() else null,
                )

            if (!conversation.isMock) ConversationCellViewDataCache.cache(cacheQuery, conversation.id.key, data)
            return data
        }

        /**
         * Returns the identifier of the most recent message in
         * [conversation] whose text contains [query], or `null` if none
         * match.
         */
        fun focusedMessageID(
            conversation: Conversation,
            query: String,
        ): String? {
            if (query.isBlank()) return null
            return conversation.messages
                .orEmpty()
                .sortedBy { it.sentDate.time }
                .lastOrNull { it.matchesSearchQuery(query) }
                ?.id
        }

        /**
         * Returns whether [conversation] matches the given search
         * query by title or by any message's text content.
         */
        fun matches(
            conversation: Conversation,
            query: String,
        ): Boolean {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return true
            if (title(conversation).lowercase().contains(trimmed.lowercase())) return true
            return conversation.messages?.any { it.matchesSearchQuery(trimmed) } == true
        }

        // MARK: - Auxiliary

        private fun Message.matchesSearchQuery(query: String): Boolean {
            val lowercased = query.lowercase()
            return translations?.any {
                it.input.value
                    .lowercase()
                    .contains(lowercased) ||
                    it.output.lowercase().contains(lowercased)
            } == true
        }

        /** The resolved title (`titleLabelText`) for the given conversation. */
        internal fun title(conversation: Conversation): String {
            val metadataName = conversation.metadata.name
            if (!metadataName.isBangQualifiedEmpty && metadataName.isNotBlank()) return metadataName

            val users = conversation.users.orEmpty()
            val firstUser = users.firstOrNull() ?: return "Unknown"
            val base = firstUser.displayName
            return if (users.size > 1) "$base + ${users.size - 1}" else base
        }

        private suspend fun subtitle(
            lastMessage: Message?,
            languageCode: String,
        ): String {
            lastMessage ?: return ""
            return when (val contentType = lastMessage.contentType) {
                is HostedContentType.Audio -> "🔊 ${LocalizedStringKey.AudioMessage.localized()}"
                is HostedContentType.Media ->
                    when {
                        contentType.fileExtension.isDocument -> "📄 ${LocalizedStringKey.Document.localized()}"
                        contentType.fileExtension.isImage -> "🏞️ ${LocalizedStringKey.Image.localized()}"
                        contentType.fileExtension.isVideo -> "🎥 ${LocalizedStringKey.Video.localized()}"
                        else -> "📎 ${LocalizedStringKey.Attachment.localized()}"
                    }
                HostedContentType.Text -> lastMessage.resolvedText(languageCode)
            }
        }

        private fun initials(title: String): String {
            val words = title.split(" ").filter { it.firstOrNull()?.isLetter() == true }
            return when {
                words.isEmpty() -> title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
                words.size == 1 -> words[0].take(1).uppercase()
                else -> (words[0].take(1) + words.last().take(1)).uppercase()
            }
        }

        private fun relativeDateString(date: Date): String {
            val calendar = Calendar.getInstance()
            val now = calendar.time
            calendar.time = date

            val messageDay = Calendar.getInstance().apply { time = date }
            val today = Calendar.getInstance().apply { time = now }

            return when {
                isSameDay(messageDay, today) -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                daysBetween(messageDay, today) < DAYS_IN_WEEK ->
                    SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("M/d/yy", Locale.getDefault()).format(date)
            }
        }

        private fun isSameDay(
            left: Calendar,
            right: Calendar,
        ): Boolean =
            left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
                left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)

        private fun daysBetween(
            earlier: Calendar,
            later: Calendar,
        ): Long {
            val difference = later.timeInMillis - earlier.timeInMillis
            return difference / MILLIS_PER_DAY
        }

        private const val CACHE_QUERY_EMPTY = "!"
        private const val DAYS_IN_WEEK = 7
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}

/**
 * Manages the in-memory conversation-cell view-data cache, keyed by
 * search query and then conversation identifier. Mirrors the iOS
 * `ConversationCellViewDataCache`. Data derived for a mock conversation
 * is never cached.
 */
object ConversationCellViewDataCache {
    // MARK: - Properties

    private val cachedDataByConversationIDForSearchQueries =
        LockIsolated<Map<String, Map<String, ConversationCellViewData>>?>(null)

    // MARK: - Methods

    /** Removes every cached conversation-cell view data. */
    fun clearCache() {
        cachedDataByConversationIDForSearchQueries.wrappedValue = null
    }

    /**
     * Removes the cached view data for the given conversation
     * identifiers across every search query.
     *
     * @param conversationIDKeys The identifiers of the conversations
     *   whose cached view data to remove.
     */
    fun removeValues(conversationIDKeys: Set<String>) {
        val cache = cachedDataByConversationIDForSearchQueries.wrappedValue ?: return
        cachedDataByConversationIDForSearchQueries.wrappedValue =
            cache.mapValues { (_, dataByID) -> dataByID.filterKeys { it !in conversationIDKeys } }
    }

    internal fun cachedValue(
        searchQuery: String,
        conversationIDKey: String,
    ): ConversationCellViewData? = cachedDataByConversationIDForSearchQueries.wrappedValue?.get(searchQuery)?.get(conversationIDKey)

    internal fun cache(
        searchQuery: String,
        conversationIDKey: String,
        data: ConversationCellViewData,
    ) {
        val cache = (cachedDataByConversationIDForSearchQueries.wrappedValue ?: emptyMap()).toMutableMap()
        val dataByID = (cache[searchQuery] ?: emptyMap()).toMutableMap()
        dataByID[conversationIDKey] = data
        cache[searchQuery] = dataByID
        cachedDataByConversationIDForSearchQueries.wrappedValue = cache
    }
}
