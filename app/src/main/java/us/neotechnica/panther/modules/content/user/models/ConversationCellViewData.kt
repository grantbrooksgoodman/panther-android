//
//  ConversationCellViewData.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.message.models.HostedContentType
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.session.extensions.isFromCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.isReadByCurrentUser
import us.neotechnica.panther.networking.modules.session.extensions.messages
import us.neotechnica.panther.networking.modules.session.extensions.resolvedText
import us.neotechnica.panther.networking.modules.session.extensions.users
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
) {
    companion object {
        /** Builds the cell data for [conversation], resolving text into [languageCode]. */
        suspend fun build(
            conversation: Conversation,
            languageCode: String,
        ): ConversationCellViewData {
            val title = title(conversation)
            val lastMessage = conversation.messages?.maxByOrNull { it.sentDate.time }

            return ConversationCellViewData(
                title = title,
                subtitle = subtitle(lastMessage, languageCode),
                dateLabelText =
                    lastMessage?.sentDate?.let { relativeDateString(it) }
                        ?: relativeDateString(conversation.metadata.lastModifiedDate),
                isShowingUnreadIndicator =
                    lastMessage != null && !lastMessage.isFromCurrentUser && !lastMessage.isReadByCurrentUser,
                initials = initials(title),
            )
        }

        // MARK: - Auxiliary

        private fun title(conversation: Conversation): String {
            val metadataName = conversation.metadata.name
            if (!metadataName.isBangQualifiedEmpty && metadataName.isNotBlank()) return metadataName

            val users = conversation.users.orEmpty()
            val firstUser = users.firstOrNull() ?: return "Unknown"
            val base = firstUser.phoneNumber.formattedString()
            return if (users.size > 1) "$base + ${users.size - 1}" else base
        }

        private suspend fun subtitle(
            lastMessage: Message?,
            languageCode: String,
        ): String {
            lastMessage ?: return ""
            return when (lastMessage.contentType) {
                HostedContentType.Text -> lastMessage.resolvedText(languageCode)
                else -> "📎 Attachment"
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

        private const val DAYS_IN_WEEK = 7
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
