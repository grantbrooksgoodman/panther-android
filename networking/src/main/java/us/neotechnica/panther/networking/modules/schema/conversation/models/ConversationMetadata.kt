//
//  ConversationMetadata.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import android.util.Base64
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import java.util.Date

/**
 * The descriptive metadata attached to a conversation.
 */
class ConversationMetadata(
    /** The conversation's display name. */
    val name: String,
    /** The conversation's image bytes, or `null` if none. */
    val imageData: ByteArray?,
    /** The content hash of the conversation's image, or `null`. */
    val imageHash: String?,
    /** A Boolean value that indicates whether this is a PenPals conversation. */
    val isPenPalsConversation: Boolean,
    /** The date the conversation was last modified. */
    val lastModifiedDate: Date,
    /** The consent-acknowledgement records for the conversation's participants. */
    val messageRecipientConsentAcknowledgementData: List<MessageRecipientConsentAcknowledgementData>,
    /** The PenPals sharing records for the conversation's participants. */
    val penPalsSharingData: List<PenPalsSharingData>,
    /**
     * The identifier of the participant whose consent the
     * conversation requires, or `null` if it requires none.
     */
    val requiresConsentFromInitiator: String?,
) : Serializable<Map<String, Any?>> {
    // MARK: - Type Aliases

    internal enum class Keys(
        val rawValue: String,
    ) {
        IMAGE_DATA("imageData"),
        IMAGE_HASH("imageHash"),
        IS_PEN_PALS_CONVERSATION("isPenPalsConversation"),
        LAST_MODIFIED_DATE("lastModified"),
        MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA("messageRecipientConsentAcknowledgementData"),
        NAME("name"),
        PEN_PALS_SHARING_DATA("penPalsSharingData"),
        REQUIRES_CONSENT_FROM_INITIATOR("requiresConsentFromInitiator"),
    }

    // MARK: - Methods

    /**
     * Returns a copy of the metadata with the given fields replaced.
     *
     * Every parameter defaults to the current value, so unspecified
     * fields are preserved; pass an explicit `null` to clear a nullable
     * field (for example, [requiresConsentFromInitiator]).
     */
    @Suppress("LongParameterList")
    fun copyWith(
        name: String = this.name,
        imageData: ByteArray? = this.imageData,
        imageHash: String? = this.imageHash,
        isPenPalsConversation: Boolean = this.isPenPalsConversation,
        lastModifiedDate: Date = this.lastModifiedDate,
        messageRecipientConsentAcknowledgementData: List<MessageRecipientConsentAcknowledgementData> =
            this.messageRecipientConsentAcknowledgementData,
        penPalsSharingData: List<PenPalsSharingData> = this.penPalsSharingData,
        requiresConsentFromInitiator: String? = this.requiresConsentFromInitiator,
    ): ConversationMetadata =
        ConversationMetadata(
            name = name,
            imageData = imageData,
            imageHash = imageHash,
            isPenPalsConversation = isPenPalsConversation,
            lastModifiedDate = lastModifiedDate,
            messageRecipientConsentAcknowledgementData = messageRecipientConsentAcknowledgementData,
            penPalsSharingData = penPalsSharingData,
            requiresConsentFromInitiator = requiresConsentFromInitiator,
        )

    // MARK: - Computed Properties

    /** The serialized representation of the conversation metadata. */
    override val encoded: Map<String, Any?>
        get() {
            val result =
                mutableMapOf<String, Any?>(
                    Keys.IMAGE_DATA.rawValue to (
                        imageData?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                            ?: BANG_QUALIFIED_EMPTY
                    ),
                    Keys.IS_PEN_PALS_CONVERSATION.rawValue to isPenPalsConversation,
                    Keys.LAST_MODIFIED_DATE.rawValue to
                        DependencyValues.current.timestampDateFormatter.format(lastModifiedDate),
                    Keys.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA.rawValue to
                        messageRecipientConsentAcknowledgementData.map { it.encoded }.sorted(),
                    Keys.NAME.rawValue to name,
                    Keys.PEN_PALS_SHARING_DATA.rawValue to
                        penPalsSharingData.map { it.encoded }.sorted(),
                    Keys.REQUIRES_CONSENT_FROM_INITIATOR.rawValue to
                        (requiresConsentFromInitiator ?: BANG_QUALIFIED_EMPTY),
                )

            imageHash?.let { result[Keys.IMAGE_HASH.rawValue] = it }
            return result
        }

    // MARK: - Companion

    companion object : SerializableDecoder<ConversationMetadata, Map<String, Any?>> {
        /**
         * Returns empty metadata for a new conversation among [userIDs].
         *
         * @param userIDs The conversation's participant identifiers.
         * @param isPenPalsConversation Whether the conversation is a PenPals conversation.
         * @param consentAcknowledged The initial consent-acknowledgement value for every participant.
         * @param requiresConsentFromInitiator The initiator whose consent the conversation
         *   requires, or `null` if none.
         */
        fun empty(
            userIDs: List<String>,
            isPenPalsConversation: Boolean,
            consentAcknowledged: Boolean,
            requiresConsentFromInitiator: String?,
        ): ConversationMetadata =
            ConversationMetadata(
                name = BANG_QUALIFIED_EMPTY,
                imageData = null,
                imageHash = null,
                isPenPalsConversation = isPenPalsConversation,
                lastModifiedDate = Date(0),
                messageRecipientConsentAcknowledgementData =
                    MessageRecipientConsentAcknowledgementData.prepopulated(userIDs, consentAcknowledged),
                penPalsSharingData = PenPalsSharingData.empty(userIDs),
                requiresConsentFromInitiator = requiresConsentFromInitiator,
            )

        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[Keys.NAME.rawValue] !is String) return false
            val imageDataString = data[Keys.IMAGE_DATA.rawValue] as? String ?: return false
            if (data[Keys.IS_PEN_PALS_CONVERSATION.rawValue] !is Boolean) return false
            val lastModifiedString = data[Keys.LAST_MODIFIED_DATE.rawValue] as? String ?: return false
            if (DependencyValues.current.timestampDateFormatter.parse(lastModifiedString) == null) {
                return false
            }

            val consentData =
                stringList(data, Keys.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA)
                    ?: return false
            val sharingData = stringList(data, Keys.PEN_PALS_SHARING_DATA) ?: return false

            val imageDecodes =
                imageDataString.isBangQualifiedEmpty ||
                    decodeBase64(imageDataString) != null

            return imageDecodes &&
                consentData.all { MessageRecipientConsentAcknowledgementData.canDecode(it) } &&
                sharingData.all { PenPalsSharingData.canDecode(it) } &&
                consentData.size == sharingData.size &&
                data[Keys.REQUIRES_CONSENT_FROM_INITIATOR.rawValue] is String
        }

        override suspend fun decode(data: Map<String, Any?>): ConversationMetadata {
            val name = data[Keys.NAME.rawValue] as? String
            val imageDataString = data[Keys.IMAGE_DATA.rawValue] as? String
            val isPenPalsConversation = data[Keys.IS_PEN_PALS_CONVERSATION.rawValue] as? Boolean
            val lastModifiedString = data[Keys.LAST_MODIFIED_DATE.rawValue] as? String
            val lastModifiedDate =
                lastModifiedString?.let {
                    DependencyValues.current.timestampDateFormatter.parse(it)
                }
            val encodedConsent = stringList(data, Keys.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA)
            val encodedSharing = stringList(data, Keys.PEN_PALS_SHARING_DATA)
            val requiresConsent = data[Keys.REQUIRES_CONSENT_FROM_INITIATOR.rawValue] as? String

            if (name == null ||
                imageDataString == null ||
                isPenPalsConversation == null ||
                lastModifiedDate == null ||
                encodedConsent == null ||
                encodedSharing == null ||
                requiresConsent == null
            ) {
                throw decodingFailure(this, data)
            }

            val imageData =
                if (imageDataString.isBangQualifiedEmpty) {
                    null
                } else {
                    decodeBase64(imageDataString) ?: throw decodingFailure(this, data)
                }

            return ConversationMetadata(
                name = name,
                imageData = imageData,
                imageHash = data[Keys.IMAGE_HASH.rawValue] as? String,
                isPenPalsConversation = isPenPalsConversation,
                lastModifiedDate = lastModifiedDate,
                messageRecipientConsentAcknowledgementData =
                    encodedConsent.map {
                        MessageRecipientConsentAcknowledgementData.decode(it)
                    },
                penPalsSharingData = encodedSharing.map { PenPalsSharingData.decode(it) },
                requiresConsentFromInitiator =
                    if (requiresConsent.isBangQualifiedEmpty) {
                        null
                    } else {
                        requiresConsent
                    },
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

        private fun decodeBase64(string: String): ByteArray? =
            runCatching {
                Base64.decode(string, Base64.NO_WRAP)
            }.getOrNull()
    }
}
