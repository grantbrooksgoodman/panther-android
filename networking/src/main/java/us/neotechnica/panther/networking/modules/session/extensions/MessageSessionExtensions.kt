//
//  MessageSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.common.models.CommonConstants
import us.neotechnica.panther.networking.modules.message.services.MediaMessageService
import us.neotechnica.panther.networking.modules.schema.conversation.models.Reaction
import us.neotechnica.panther.networking.modules.schema.message.models.LocalMediaFilePath
import us.neotechnica.panther.networking.modules.schema.message.models.MediaFile
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.message.models.ReadReceipt
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.models.OutboxEntry
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.translation.services.TranslationResolver
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/** Whether the message was sent by the current user. */
val Message.isFromCurrentUser: Boolean
    get() = fromAccountID == User.currentUserID

/** Whether the message carries media content (an image, video, or document). */
val Message.isMediaMessage: Boolean
    get() = contentType.isMedia

/**
 * Resolves the message's media file, using the local copy when
 * available and downloading it otherwise, or `null` if the message is
 * not media or the resolution fails.
 */
suspend fun Message.resolvedMediaFile(): MediaFile? {
    val localMediaFilePath = LocalMediaFilePath.from(this) ?: return null
    return runCatching { MediaMessageService.getMediaComponent(id, localMediaFilePath) }.getOrNull()
}

/**
 * The message's already-downloaded media file, or `null` if the message
 * is not media or its media has not yet been downloaded. Does not
 * trigger a download.
 */
val Message.cachedMediaFile: MediaFile?
    get() {
        val localMediaFilePath = LocalMediaFilePath.from(this) ?: return null
        return MediaFile.from(localMediaFilePath.relativePathString)
    }

/** Whether the current user has read the message. */
val Message.isReadByCurrentUser: Boolean
    get() = readReceipts?.any { it.userID == User.currentUserID } == true

/** Whether the message is a mock, representing a message not yet sent. */
val Message.isMock: Boolean
    get() = id == CommonConstants.NEW_MESSAGE_ID

/** Whether the message is staged in the outbox awaiting delivery. */
val Message.isOutboxMessage: Boolean
    get() = id.startsWith(OutboxEntry.ID_PREFIX)

/** Whether the message is a system message, such as an activity notice. */
val Message.isSystemMessage: Boolean
    get() = fromAccountID == CommonConstants.SYSTEM_MESSAGE_ID

/** The other participant's read receipt, if any (for delivery status). */
val Message.otherParticipantReadReceipt: ReadReceipt?
    get() = readReceipts?.firstOrNull { it.userID != User.currentUserID }

/**
 * The reactions applied to this message, or `null` when none.
 *
 * Resolved from the current conversation's reaction metadata, mirroring
 * the iOS `Message.reactions`.
 */
val Message.reactions: List<Reaction>?
    get() =
        ConversationSessionService.currentConversation
            ?.reactionMetadata
            ?.firstOrNull { it.messageID == id }
            ?.reactions

/**
 * The message's full translation resolved for display in [languageCode].
 *
 * Prefers the inline [Message.translations] (carried by locally built,
 * outbox, and mock messages); otherwise resolves the best-matching
 * reference through the hosted archive. Returns `null` when the message
 * carries no resolvable translation.
 */
suspend fun Message.resolvedTranslation(languageCode: String): Translation? {
    translations?.let { inline ->
        return pickInlineTranslation(inline, languageCode) ?: inline.firstOrNull()
    }

    val parsed =
        translationReferences
            .orEmpty()
            .mapNotNull { HostedTranslationReference.fromString(it.hostingKey) }
    if (parsed.isEmpty()) return null

    val reference =
        if (isFromCurrentUser) {
            parsed.firstOrNull { it.languagePair.from == languageCode } ?: parsed.first()
        } else {
            parsed.firstOrNull { it.languagePair.to == languageCode } ?: parsed.first()
        }

    return runCatching { TranslationResolver.resolve(reference) }.getOrNull()
}

private fun Message.pickInlineTranslation(
    inline: List<Translation>,
    languageCode: String,
): Translation? =
    if (isFromCurrentUser) {
        inline.firstOrNull { it.languagePair.from == languageCode }
    } else {
        inline.firstOrNull { it.languagePair.to == languageCode }
    }

/**
 * The message's text resolved into [languageCode], for previews.
 *
 * Mirrors the chat bubble: for a message the current user sent, the text
 * in their language is the translation's input (its output is the
 * recipient's language); for a received message it is the output
 * translated into [languageCode]. Returns an empty string when the
 * message carries no resolvable text.
 */
suspend fun Message.resolvedText(languageCode: String): String {
    val translation = resolvedTranslation(languageCode) ?: return ""
    return if (isFromCurrentUser) translation.input.value else translation.output
}
