//
//  SessionTestEnvironment.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.support

import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.networking.conversation.models.ConversationMetadata
import us.neotechnica.panther.modules.networking.conversation.models.Participant
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import java.util.Date

/**
 * Builders for the session-layer fixtures the Phase 6 tests share:
 * users with distinct phone numbers, text messages, and conversations
 * whose metadata is empty.
 */
object SessionTestEnvironment {
    // MARK: - Builders

    /** A conversation between the given participants with the given message identifiers. */
    fun conversation(
        key: String,
        participantIDs: List<String>,
        messageIDs: List<String> = emptyList(),
        hash: String = "hash-$key",
    ): Conversation =
        Conversation(
            id = ConversationID(key = key, hash = hash),
            activities = null,
            messageIDs = messageIDs.ifEmpty { bangQualifiedEmptyList },
            metadata = ConversationMetadata.empty(userIDs = participantIDs),
            participants = participantIDs.map { Participant(userID = it) },
            reactionMetadata = null,
        )

    /** A text message with no translations. */
    fun message(
        id: String,
        fromAccountID: String,
        sentDate: Date,
    ): Message =
        Message(
            id = id,
            fromAccountID = fromAccountID,
            contentType = HostedContentType.Text,
            richContent = null,
            translationReferences = null,
            translations = null,
            readReceipts = null,
            sentDate = sentDate,
        )

    /** A user with the given identifier, language, and national phone number. */
    fun user(
        id: String,
        languageCode: String = "en",
        nationalNumber: String = "5551234567",
        blockedUserIDs: List<String>? = null,
        conversationIDs: List<ConversationID>? = null,
    ): User =
        User(
            id = id,
            aiEnhancedTranslationsEnabled = false,
            blockedUserIDs = blockedUserIDs,
            conversationIDs = conversationIDs,
            deviceID = "device",
            isPenPalsParticipant = false,
            languageCode = languageCode,
            messageRecipientConsentRequired = false,
            phoneNumber = PhoneNumber(nationalNumber),
            previousLanguageCodes = null,
            pushTokens = null,
        )
}
