//
//  Message.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.networking.conversation.models.Reaction
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.translator.models.Translation
import java.util.Date

/**
 * A message in a conversation.
 *
 * A message carries its sender, content type, sent date, and –
 * depending on its type – rich audio or media content,
 * translations, and read receipts. Text messages carry
 * translations; audio and media messages carry rich content
 * resolved from remote storage.
 */
data class Message(
    /** The message's unique identifier. */
    val id: String,
    /** The identifier of the account that sent the message. */
    val fromAccountID: String,
    /** The message's content type. */
    val contentType: HostedContentType,
    /** The message's rich content – audio or media – if any. */
    val richContent: RichMessageContent?,
    /** The references to the message's translations. */
    val translationReferences: List<TranslationReference>?,
    /** The message's resolved translations. */
    val translations: List<Translation>?,
    /** The message's read receipts, or `null` if it has none. */
    val readReceipts: List<ReadReceipt>?,
    /** The date the message was sent. */
    val sentDate: Date,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Types

    /** The serializable keys for encoding and decoding a message. */
    enum class SerializableKey(
        val rawValue: String,
    ) {
        ID("id"),
        FROM_ACCOUNT_ID("fromAccount"),
        CONTENT_TYPE("contentType"),
        TRANSLATION_REFERENCES("translations"),
        READ_RECEIPTS("readReceipts"),
        SENT_DATE("sentDate"),
    }

    // MARK: - Computed Properties

    /** The message's first audio component, or `null` if it has none. */
    val audioComponent: AudioMessageReference?
        get() = audioComponents?.firstOrNull()

    /** The message's audio components, or `null` if it has none. */
    val audioComponents: List<AudioMessageReference>?
        get() = richContent?.audioComponents

    /**
     * The current user's read receipt for the message, or `null` if
     * the user has not read it.
     */
    val currentUserReadReceipt: ReadReceipt?
        get() = readReceipts?.firstOrNull { it.userID == User.currentUserID }

    /** The message's document, or `null` if its content is not a document. */
    val documentComponent: MediaFile?
        get() = richContent?.documentComponent

    /** The message's image, or `null` if its content is not an image. */
    val imageComponent: MediaFile?
        get() = richContent?.imageComponent

    /**
     * The local file path for the message's audio, or `null` if it is
     * not an audio message.
     */
    val localAudioFilePath: LocalAudioFilePath?
        get() = LocalAudioFilePath.from(this)

    /**
     * The local file path for the message's media, or `null` if it is
     * not a media message.
     */
    val localMediaFilePath: LocalMediaFilePath?
        get() = LocalMediaFilePath.from(this)

    /**
     * The reactions on the message, or `null` if it has none.
     *
     * **Note:** Always returns `null` if the message is not in the
     * currently presented conversation.
     */
    val reactions: List<Reaction>?
        get() {
            val conversation = DependencyValues.current.clientSession.entity.conversation.currentConversation
            return conversation
                ?.reactionMetadata
                ?.firstOrNull { it.messageID == id }
                ?.reactions
        }

    /** The translation for this message in the current user's language code. */
    val translation: Translation?
        get() = translations?.firstOrNull()

    /** The message's video, or `null` if its content is not a video. */
    val videoComponent: MediaFile?
        get() = richContent?.videoComponent

    // MARK: - Serializable Conformance

    /** The serialized representation of the message. */
    override val encoded: Map<String, Any?>
        get() {
            val formatter = DependencyValues.current.timestampDateFormatter
            return mapOf(
                SerializableKey.ID.rawValue to id,
                SerializableKey.FROM_ACCOUNT_ID.rawValue to fromAccountID,
                SerializableKey.CONTENT_TYPE.rawValue to contentType.hostedValue,
                SerializableKey.TRANSLATION_REFERENCES.rawValue to
                    (translationReferences?.map { it.hostingKey } ?: bangQualifiedEmptyList),
                SerializableKey.READ_RECEIPTS.rawValue to
                    (readReceipts?.map { it.encoded } ?: bangQualifiedEmptyList),
                SerializableKey.SENT_DATE.rawValue to formatter.format(sentDate),
            )
        }

    // MARK: - EncodedHashable Conformance

    /**
     * The strings that collectively define this instance's identity
     * for hashing purposes, sorted alphabetically.
     */
    override val hashFactors: List<String>
        get() {
            val formatter = DependencyValues.current.timestampDateFormatter
            return buildList {
                add(id)
                add(fromAccountID)
                add(contentType.rawValue)
                add(formatter.format(sentDate))
                // Render read receipt dates with the UTC hash
                // formatter rather than the wire-format encoded
                // property, which uses the ambient timezone.
                readReceipts?.forEach {
                    add("${it.userID} | ${formatter.format(it.readDate)}")
                }
            }.sorted()
        }

    // MARK: - Companion

    companion object {
        /** An empty message placeholder. */
        val empty: Message =
            Message(
                id = "",
                fromAccountID = "",
                contentType = HostedContentType.Text,
                richContent = null,
                translationReferences = null,
                translations = null,
                readReceipts = null,
                sentDate = Date(0),
            )
    }
}
