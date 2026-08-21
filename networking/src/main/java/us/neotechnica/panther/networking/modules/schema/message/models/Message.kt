//
//  Message.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.message.models

import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.translator.models.Translation
import java.util.Date

/**
 * A message within a conversation.
 *
 * **Note:** This Phase 2 port decodes the message's wire fields
 * directly. Translation resolution and audio/media content
 * download – performed during decode on iOS – are deferred to the
 * translation (Phase 4) and chat (Phase 7) phases; decoded
 * messages carry their [translationReferences] as raw hosting
 * keys.
 */
data class Message(
    /** The message's identifier. */
    val id: String,
    /** The identifier of the account that sent the message. */
    val fromAccountID: String,
    /** The kind of content the message carries. */
    val contentType: HostedContentType,
    /** The message's translation references, or `null` if none. */
    val translationReferences: List<TranslationReference>?,
    /** The message's read receipts, or `null` if none. */
    val readReceipts: List<ReadReceipt>?,
    /** The date the message was sent. */
    val sentDate: Date,
    /**
     * The message's resolved translations, or `null` if unresolved.
     *
     * This is a transient, display-only field: it is never encoded or
     * hashed (the wire format carries only [translationReferences]).
     * Locally built, outbox, and mock messages carry their freshly
     * computed translations here so they render immediately; messages
     * decoded from the wire leave it `null` and resolve on demand.
     */
    val translations: List<Translation>? = null,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
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

    /** The serialized representation of the message. */
    override val encoded: Map<String, Any?>
        get() {
            val formatter = DependencyValues.current.timestampDateFormatter
            return mapOf(
                Keys.ID.rawValue to id,
                Keys.FROM_ACCOUNT_ID.rawValue to fromAccountID,
                Keys.CONTENT_TYPE.rawValue to contentType.hostedValue,
                Keys.TRANSLATION_REFERENCES.rawValue to
                    (translationReferences?.map { it.hostingKey } ?: bangQualifiedEmptyList),
                Keys.READ_RECEIPTS.rawValue to
                    (readReceipts?.map { it.encoded } ?: bangQualifiedEmptyList),
                Keys.SENT_DATE.rawValue to formatter.format(sentDate),
            )
        }

    override val hashFactors: List<String>
        get() {
            val formatter = DependencyValues.current.timestampDateFormatter
            return buildList {
                add(id)
                add(fromAccountID)
                add(contentType.rawValue)
                add(formatter.format(sentDate))
                // Render read receipt dates with the UTC hash formatter.
                readReceipts?.forEach {
                    add("${it.userID} | ${formatter.format(it.readDate)}")
                }
            }.sorted()
        }

    // MARK: - Companion

    companion object : SerializableDecoder<Message, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[Keys.ID.rawValue] !is String) return false
            if (data[Keys.FROM_ACCOUNT_ID.rawValue] !is String) return false
            val contentTypeString = data[Keys.CONTENT_TYPE.rawValue] as? String ?: return false
            val contentType = HostedContentType.from(contentTypeString) ?: return false
            val readReceipts = stringList(data, Keys.READ_RECEIPTS) ?: return false
            if (!readReceipts.isBangQualifiedEmpty && !readReceipts.all { ReadReceipt.canDecode(it) }) {
                return false
            }

            val sentDateString = data[Keys.SENT_DATE.rawValue] as? String ?: return false
            if (DependencyValues.current.timestampDateFormatter.parse(sentDateString) == null) {
                return false
            }

            val translations = stringList(data, Keys.TRANSLATION_REFERENCES) ?: return false
            return !(contentType == HostedContentType.Text && translations.isBangQualifiedEmpty)
        }

        override suspend fun decode(data: Map<String, Any?>): Message {
            val id = data[Keys.ID.rawValue] as? String
            val fromAccountID = data[Keys.FROM_ACCOUNT_ID.rawValue] as? String
            val contentTypeString = data[Keys.CONTENT_TYPE.rawValue] as? String
            val contentType = contentTypeString?.let { HostedContentType.from(it) }
            val translationStrings = stringList(data, Keys.TRANSLATION_REFERENCES)
            val encodedReadReceipts = stringList(data, Keys.READ_RECEIPTS)
            val sentDateString = data[Keys.SENT_DATE.rawValue] as? String
            val sentDate =
                sentDateString?.let {
                    DependencyValues.current.timestampDateFormatter.parse(it)
                }

            if (id == null ||
                fromAccountID == null ||
                contentType == null ||
                translationStrings == null ||
                encodedReadReceipts == null ||
                sentDate == null
            ) {
                throw decodingFailure(this, data)
            }

            val readReceipts =
                if (encodedReadReceipts.isBangQualifiedEmpty) {
                    null
                } else {
                    encodedReadReceipts.map { ReadReceipt.decode(it) }
                }

            val translationReferences =
                translationStrings
                    .takeUnless { it.isBangQualifiedEmpty }
                    ?.mapNotNull { TranslationReference.from(it) }

            return Message(
                id = id,
                fromAccountID = fromAccountID,
                contentType = contentType,
                translationReferences = translationReferences,
                readReceipts = readReceipts,
                sentDate = sentDate,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun stringList(
            data: Map<String, Any?>,
            key: Keys,
        ): List<String>? =
            (data[key.rawValue] as? List<*>)
                ?.takeIf { list -> list.all { it is String } }
                ?.map { it as String }
    }
}
