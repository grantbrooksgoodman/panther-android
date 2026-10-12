//
//  Message+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewStrings
import us.neotechnica.panther.modules.content.user.constants.SystemMessageCellFloats
import us.neotechnica.panther.modules.content.user.dependencies.contextMenuActionHandlerService
import us.neotechnica.panther.modules.content.user.models.MessageKind
import us.neotechnica.panther.modules.content.user.services.AlternateMessageService
import us.neotechnica.panther.modules.content.user.services.AudioMessagePlaybackService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
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
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.localization.models.localized
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
 * A Boolean value that indicates whether this message acknowledges a
 * message receipt consent request.
 */
val Message.isConsentAcknowledgementMessage: Boolean
    get() {
        val translation = translation ?: return false
        return translation.input.value ==
            LocalizedStringKey.MessageRecipientConsentAcknowledgementMessage.localized(
                languageCode = translation.languagePair.from,
            )
    }

/**
 * A Boolean value that indicates whether this message is a message
 * receipt consent request or acknowledgement.
 */
val Message.isConsentMessage: Boolean
    get() = isConsentAcknowledgementMessage || isConsentRequestMessage

/**
 * A Boolean value that indicates whether this message requests
 * message receipt consent.
 */
val Message.isConsentRequestMessage: Boolean
    get() {
        Message.consentRequestMessageID?.let { return id == it }
        val currentConversation = DependencyValues.current.clientSession.entity.conversation.currentConversation

        val translation = translation ?: return false
        val inputMatches =
            translation.input.value ==
                LocalizedStringKey.MessageRecipientConsentRequestMessage.localized(
                    languageCode = translation.languagePair.from,
                )

        // A non-matching message can never be a consent request, so
        // short-circuit before resolving the current conversation's
        // messages – an expensive lookup to run per message.
        if (!inputMatches) return false
        currentConversation ?: return inputMatches
        val firstMessageID =
            currentConversation.messages?.firstOrNull()?.id
                ?: currentConversation.messageIDs.firstOrNull()
        val isConsentMessage = inputMatches && (id == CommonConstants.NEW_MESSAGE_ID || id == firstMessageID)
        if (isConsentMessage && id != CommonConstants.NEW_MESSAGE_ID) Message.consentRequestMessageID = id

        return isConsentMessage
    }

/**
 * A Boolean value that indicates whether the message is a system
 * message, such as a conversation activity notice.
 */
val Message.isSystemMessage: Boolean
    get() = fromAccountID == CommonConstants.SYSTEM_MESSAGE_ID

/**
 * The identifier of the conversation's message receipt consent
 * request message, if known.
 */
var Message.Companion.consentRequestMessageID: String?
    get() = ConsentRequestMessageIDCache.wrappedValue
    set(value) {
        ConsentRequestMessageIDCache.wrappedValue = value
    }

private val ConsentRequestMessageIDCache = LockIsolated<String?>(null)

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

// MARK: - Display

/**
 * The attributed string for a system message, combining its date
 * separator and activity text, or `null` if the message is not a
 * system message. Participant names wrapped in `⌘…⌘` sentinels render
 * bold; the caller supplies the color.
 */
val Message.attributedSystemString: AnnotatedString?
    get() {
        if (!isSystemMessage) return null
        val text = systemLocalized.translation?.output ?: return null
        val dateString = sentDate.chatPageMessageSeparatorAttributedDateString ?: return null

        return buildAnnotatedString {
            append(dateString)
            append("\n")
            val activityStart = length
            val sanitizedText = text.sanitized
            append(sanitizedText)
            addStyle(
                SpanStyle(fontSize = SystemMessageCellFloats.ACTIVITY_STRING_SYSTEM_FONT_SIZE.sp),
                activityStart,
                activityStart + sanitizedText.length,
            )

            // Map each sentinel-wrapped range onto the sanitized text.
            var sanitizedOffset = 0
            var isBold = false
            text.split("⌘").forEach { segment ->
                val cleanedSegment = segment.sanitized
                if (isBold && cleanedSegment.isNotEmpty()) {
                    addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = SystemMessageCellFloats.ACTIVITY_STRING_SYSTEM_FONT_SIZE.sp,
                        ),
                        activityStart + sanitizedOffset,
                        activityStart + sanitizedOffset + cleanedSegment.length,
                    )
                }
                sanitizedOffset += cleanedSegment.length
                isBold = !isBold
            }
        }
    }

/**
 * A Boolean value that indicates whether this message's audio is
 * currently playing.
 */
val Message.isPlayingMessage: Boolean
    get() {
        if (!contentType.isAudio || audioComponent == null) return false
        val playingMessageID = AudioMessagePlaybackService.playingMessageID ?: return false
        return playingMessageID == id
    }

/**
 * A Boolean value that indicates whether this message is currently
 * being spoken aloud.
 */
val Message.isSpeakingMessage: Boolean
    get() {
        val speakingMessage = DependencyValues.current.contextMenuActionHandlerService.speakingMessage ?: return false
        return speakingMessage.id == id
    }

/**
 * The message's content, as represented for display in the message
 * list.
 *
 * Maps the message to text, attributed text, audio, photo, or video
 * based on its content type and the alternate content it currently
 * displays.
 */
val Message.kind: MessageKind
    get() {
        if (isSystemMessage) return MessageKind.Custom

        if (contentType.isAudio) {
            val audioComponent = audioComponent
            val translation = translation
            if (audioComponent != null && translation != null) {
                if (!AlternateMessageService.isDisplayingAudioTranscription(this)) {
                    return MessageKind.Audio(if (isFromCurrentUser) audioComponent.original else audioComponent.translated)
                }

                return MessageKind.AttributedText(
                    (if (isFromCurrentUser) translation.input.value else translation.output).sanitized,
                    isItalicized = true,
                )
            }
        } else if (contentType.isMedia) {
            documentComponent?.let { return MessageKind.Photo(it) }
            imageComponent?.let { return MessageKind.Photo(it) }
            videoComponent?.let { return MessageKind.Video(it) }
        }

        val translation = translation ?: return MessageKind.Text(ChatPageViewStrings.MessagesDataSource.MISSING_TRANSLATION_TEXT)

        val isDisplayingAlternateText = AlternateMessageService.isDisplayingAlternateText(this)
        val primaryText = if (isFromCurrentUser) translation.input.value else translation.output
        val alternateText = if (isFromCurrentUser) translation.output else translation.input.value

        val consentAcknowledgementMessage = LocalizedStringKey.MessageRecipientConsentAcknowledgementMessage.localized()
        val consentRequestMessage = LocalizedStringKey.MessageRecipientConsentRequestMessage.localized()

        val resolvedText =
            if (isConsentMessage) {
                (if (isConsentAcknowledgementMessage) consentAcknowledgementMessage else consentRequestMessage).sanitized.trim()
            } else {
                if (isDisplayingAlternateText) alternateText else primaryText
            }

        return MessageKind.AttributedText(resolvedText.sanitized, isItalicized = isConsentMessage || isDisplayingAlternateText)
    }

/**
 * The text the message's label displays, or `null` if the message
 * renders no text.
 */
val Message.messageLabelText: String?
    get() =
        when (val kind = kind) {
            is MessageKind.AttributedText -> kind.text
            is MessageKind.Text -> kind.text
            else -> null
        }
