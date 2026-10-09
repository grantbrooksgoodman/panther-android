//
//  Message+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.message.models.ReadReceipt
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

// MARK: - Properties

/** A Boolean value that indicates whether the message was sent by the current user. */
val Message.isFromCurrentUser: Boolean
    get() = fromAccountID == User.currentUserID

/**
 * A Boolean value that indicates whether the message is an outbox
 * message whose delivery failed.
 */
val Message.isFailedOutboxMessage: Boolean
    get() {
        if (!isOutboxMessage) return false
        val outbox = DependencyValues.current.clientSession.outbox
        return outbox.entry(id)?.state == OutboxEntry.State.FAILED
    }

/**
 * A Boolean value that indicates whether the message is a mock,
 * representing a message not yet sent.
 */
val Message.isMock: Boolean
    get() = id == CommonConstants.NEW_MESSAGE_ID

/**
 * A Boolean value that indicates whether the message is staged in
 * the outbox awaiting delivery.
 */
val Message.isOutboxMessage: Boolean
    get() = id.startsWith(OutboxEntry.ID_PREFIX)

/** Whether the current user has read the message. */
val Message.isReadByCurrentUser: Boolean
    get() = readReceipts?.any { it.userID == User.currentUserID } == true

/**
 * A Boolean value that indicates whether the message is a system
 * message, such as a conversation activity notice.
 */
val Message.isSystemMessage: Boolean
    get() = fromAccountID == CommonConstants.SYSTEM_MESSAGE_ID

/** The other participant's read receipt, if any (for delivery status). */
val Message.otherParticipantReadReceipt: ReadReceipt?
    get() = readReceipts?.firstOrNull { it.userID != User.currentUserID }

/**
 * A copy of this system message hydrated with its localized activity
 * text, or the message itself if it is not a system message.
 */
val Message.systemLocalized: Message
    get() {
        val conversation = DependencyValues.current.clientSession.entity.conversation.currentConversation
        if (!isSystemMessage) return this
        val activity = conversation?.activities?.firstOrNull { id == it.encodedHash } ?: return this
        return Message(
            id = activity.encodedHash,
            fromAccountID = CommonConstants.SYSTEM_MESSAGE_ID,
            contentType = HostedContentType.Text,
            richContent = null,
            translationReferences =
                listOf(
                    TranslationReference(
                        languagePair = LanguagePair.system,
                        type = TranslationReference.Type.Idempotent(activity.encodedHash),
                    ),
                ),
            translations =
                listOf(
                    Translation(
                        input = TranslationInput(activity.description),
                        output = activity.description,
                        languagePair = LanguagePair.system,
                    ),
                ),
            readReceipts = null,
            sentDate = activity.date,
        )
    }

// MARK: - Methods

/**
 * Returns a Boolean value that indicates whether the message's text
 * contains the given search term, ignoring case and surrounding
 * whitespace.
 *
 * @param searchTerm The term to search for.
 *
 * @return `true` if the message's text contains the term; otherwise,
 *   `false`.
 */
fun Message.textContains(searchTerm: String): Boolean {
    val translation = translation ?: return false
    val normalizedSearchTerm = searchTerm.lowercase().trim()
    val comparator = if (isFromCurrentUser) translation.input.value else translation.output.sanitized
    return comparator.lowercase().trim().contains(normalizedSearchTerm)
}
