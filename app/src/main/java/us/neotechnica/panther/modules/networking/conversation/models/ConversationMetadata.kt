//
//  ConversationMetadata.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import android.util.Base64
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import java.security.MessageDigest
import java.util.Date

/**
 * The metadata that describes a conversation.
 *
 * @param name The conversation's name.
 * @param imageData The conversation's image data, or `null` if it has
 *   none.
 * @param imageHash The hash of the conversation's image data, or
 *   `null` to compute it from `imageData`.
 * @param isPenPalsConversation A Boolean value that indicates whether
 *   the conversation is a PenPals conversation.
 * @param lastModifiedDate The date the conversation was last modified.
 * @param messageRecipientConsentAcknowledgementData The consent
 *   acknowledgement records for the conversation's participants.
 * @param penPalsSharingData The PenPals sharing records for the
 *   conversation's participants.
 * @param requiresConsentFromInitiator The identifier of the
 *   participant whose message-receipt consent the conversation
 *   requires, or `null` if it requires none.
 */
@Suppress("LongParameterList")
class ConversationMetadata(
    /** The conversation's name. */
    val name: String,
    /** The conversation's image data, or `null` if it has none. */
    val imageData: ByteArray?,
    imageHash: String? = null,
    /** A Boolean value that indicates whether the conversation is a PenPals conversation. */
    val isPenPalsConversation: Boolean,
    /** The date the conversation was last modified. */
    val lastModifiedDate: Date,
    /** The consent acknowledgement records for the conversation's participants. */
    val messageRecipientConsentAcknowledgementData: List<MessageRecipientConsentAcknowledgementData>,
    /** The PenPals sharing records for the conversation's participants. */
    val penPalsSharingData: List<PenPalsSharingData>,
    /**
     * The identifier of the participant whose message-receipt consent
     * the conversation requires, or `null` if it requires none.
     */
    val requiresConsentFromInitiator: String?,
) : Serializable<Map<String, Any?>> {
    // MARK: - Types

    /** The serializable keys for encoding and decoding conversation metadata. */
    enum class SerializableKey(
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

    // MARK: - Properties

    /** The hash of the conversation's image data, or `null` if it has no image. */
    val imageHash: String? = imageHash ?: imageData?.let { computeImageHash(it) }

    // MARK: - Computed Properties

    /** The serialized representation of the conversation metadata. */
    override val encoded: Map<String, Any?>
        get() {
            val result =
                mutableMapOf<String, Any?>(
                    SerializableKey.IMAGE_DATA.rawValue to (
                        imageData?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                            ?: BANG_QUALIFIED_EMPTY
                    ),
                    SerializableKey.IS_PEN_PALS_CONVERSATION.rawValue to isPenPalsConversation,
                    SerializableKey.LAST_MODIFIED_DATE.rawValue to
                        DependencyValues.current.timestampDateFormatter.format(lastModifiedDate),
                    SerializableKey.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA.rawValue to
                        messageRecipientConsentAcknowledgementData.map { it.encoded }.sorted(),
                    SerializableKey.NAME.rawValue to name,
                    SerializableKey.PEN_PALS_SHARING_DATA.rawValue to
                        penPalsSharingData.map { it.encoded }.sorted(),
                    SerializableKey.REQUIRES_CONSENT_FROM_INITIATOR.rawValue to
                        (requiresConsentFromInitiator ?: BANG_QUALIFIED_EMPTY),
                )

            imageHash?.let { result[SerializableKey.IMAGE_HASH.rawValue] = it }
            return result
        }

    // MARK: - Mutation

    /**
     * Returns a copy of the metadata with the given properties
     * replaced.
     *
     * Only the properties you provide are changed; the rest are
     * copied unchanged. To clear the image data or the
     * required-consent value rather than leave it unchanged, pass
     * [nilImageData] or [nilRequiresConsentFromInitiator].
     *
     * @param name The new name, or `null` to keep the current name.
     * @param imageData The new image data, or an empty value to keep
     *   the current image data.
     * @param isPenPalsConversation The new value, or `null` to keep
     *   the current value.
     * @param lastModifiedDate The new last-modified date, or `null`
     *   to keep the current date.
     * @param messageRecipientConsentAcknowledgementData The new
     *   consent acknowledgement records, or `null` to keep the current
     *   records.
     * @param penPalsSharingData The new PenPals sharing records, or
     *   `null` to keep the current records.
     * @param requiresConsentFromInitiator The new value, or an empty
     *   string to keep the current value.
     * @param nilImageData A Boolean value that, when `true`, clears
     *   the image data.
     * @param nilRequiresConsentFromInitiator A Boolean value that,
     *   when `true`, clears the required-consent value.
     *
     * @return The updated metadata.
     */
    @Suppress("LongParameterList")
    fun copyWith(
        name: String? = null,
        imageData: ByteArray = ByteArray(0),
        isPenPalsConversation: Boolean? = null,
        lastModifiedDate: Date? = null,
        messageRecipientConsentAcknowledgementData: List<MessageRecipientConsentAcknowledgementData>? = null,
        penPalsSharingData: List<PenPalsSharingData>? = null,
        requiresConsentFromInitiator: String = "",
        nilImageData: Boolean = false,
        nilRequiresConsentFromInitiator: Boolean = false,
    ): ConversationMetadata {
        if (name == null &&
            imageData.isEmpty() &&
            isPenPalsConversation == null &&
            lastModifiedDate == null &&
            messageRecipientConsentAcknowledgementData == null &&
            penPalsSharingData == null &&
            requiresConsentFromInitiator.isEmpty() &&
            !nilImageData &&
            !nilRequiresConsentFromInitiator
        ) {
            Logger.log(
                Exception(
                    "No arguments passed to mutator method.",
                    metadata = ExceptionMetadata(this),
                ),
            )

            return this
        }

        val resolvedImageData = if (nilImageData) null else (if (imageData.isEmpty()) this.imageData else imageData)
        val resolvedRequiresConsentFromInitiator =
            if (nilRequiresConsentFromInitiator) {
                null
            } else {
                requiresConsentFromInitiator.ifEmpty { this.requiresConsentFromInitiator }
            }

        return ConversationMetadata(
            name = name ?: this.name,
            imageData = resolvedImageData,
            isPenPalsConversation = isPenPalsConversation ?: this.isPenPalsConversation,
            lastModifiedDate = lastModifiedDate ?: this.lastModifiedDate,
            messageRecipientConsentAcknowledgementData =
                messageRecipientConsentAcknowledgementData ?: this.messageRecipientConsentAcknowledgementData,
            penPalsSharingData = penPalsSharingData ?: this.penPalsSharingData,
            requiresConsentFromInitiator = resolvedRequiresConsentFromInitiator,
        )
    }

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConversationMetadata) return false
        return name == other.name &&
            imageData.contentEqualsOrBothNull(other.imageData) &&
            imageHash == other.imageHash &&
            isPenPalsConversation == other.isPenPalsConversation &&
            lastModifiedDate == other.lastModifiedDate &&
            messageRecipientConsentAcknowledgementData == other.messageRecipientConsentAcknowledgementData &&
            penPalsSharingData == other.penPalsSharingData &&
            requiresConsentFromInitiator == other.requiresConsentFromInitiator
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = HASH_MULTIPLIER * result + (imageData?.contentHashCode() ?: 0)
        result = HASH_MULTIPLIER * result + (imageHash?.hashCode() ?: 0)
        result = HASH_MULTIPLIER * result + isPenPalsConversation.hashCode()
        result = HASH_MULTIPLIER * result + lastModifiedDate.hashCode()
        result = HASH_MULTIPLIER * result + messageRecipientConsentAcknowledgementData.hashCode()
        result = HASH_MULTIPLIER * result + penPalsSharingData.hashCode()
        result = HASH_MULTIPLIER * result + (requiresConsentFromInitiator?.hashCode() ?: 0)
        return result
    }

    // MARK: - Auxiliary

    private fun ByteArray?.contentEqualsOrBothNull(other: ByteArray?): Boolean =
        when {
            this == null && other == null -> true
            this == null || other == null -> false
            else -> contentEquals(other)
        }

    // MARK: - Companion

    companion object : SerializableDecoder<ConversationMetadata, Map<String, Any?>> {
        private const val HASH_MULTIPLIER = 31

        /**
         * Returns the hash of the given image data.
         *
         * @param data The image data to hash.
         *
         * @return The hash of the image data.
         */
        fun computeImageHash(data: ByteArray): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(data)
                .joinToString("") { "%02x".format(it) }

        /**
         * Returns default metadata for a conversation with the given
         * participants.
         *
         * @param userIDs The identifiers of the conversation's
         *   participants.
         * @param isPenPalsConversation A Boolean value that indicates
         *   whether the conversation is a PenPals conversation.
         *
         * @return The default metadata.
         */
        fun empty(
            userIDs: List<String>,
            isPenPalsConversation: Boolean = false,
        ): ConversationMetadata {
            val currentUser = DependencyValues.current.clientSession.entity.user.currentUser

            var requiresConsentFromInitiatorString: String? = null
            if (currentUser != null && currentUser.messageRecipientConsentRequired) {
                requiresConsentFromInitiatorString = currentUser.id
            }

            return ConversationMetadata(
                name = BANG_QUALIFIED_EMPTY,
                imageData = null,
                isPenPalsConversation = isPenPalsConversation,
                lastModifiedDate = Date(0),
                messageRecipientConsentAcknowledgementData = MessageRecipientConsentAcknowledgementData.prepopulated(userIDs),
                penPalsSharingData =
                    if (isPenPalsConversation) {
                        PenPalsSharingData.prepopulated(userIDs)
                    } else {
                        PenPalsSharingData.empty(userIDs)
                    },
                requiresConsentFromInitiator = requiresConsentFromInitiatorString,
            )
        }

        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[SerializableKey.NAME.rawValue] !is String) return false
            val imageDataString = data[SerializableKey.IMAGE_DATA.rawValue] as? String ?: return false
            if (data[SerializableKey.IS_PEN_PALS_CONVERSATION.rawValue] !is Boolean) return false
            val lastModifiedString = data[SerializableKey.LAST_MODIFIED_DATE.rawValue] as? String ?: return false
            if (DependencyValues.current.timestampDateFormatter.parse(lastModifiedString) == null) {
                return false
            }

            val consentData =
                stringList(data, SerializableKey.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA)
                    ?: return false
            val sharingData = stringList(data, SerializableKey.PEN_PALS_SHARING_DATA) ?: return false

            val imageDecodes =
                imageDataString.isBangQualifiedEmpty ||
                    decodeBase64(imageDataString) != null

            return imageDecodes &&
                consentData.all { MessageRecipientConsentAcknowledgementData.canDecode(it) } &&
                sharingData.all { PenPalsSharingData.canDecode(it) } &&
                consentData.size == sharingData.size &&
                data[SerializableKey.REQUIRES_CONSENT_FROM_INITIATOR.rawValue] is String
        }

        override fun decode(data: Map<String, Any?>): ConversationMetadata {
            val name = data[SerializableKey.NAME.rawValue] as? String
            val imageDataString = data[SerializableKey.IMAGE_DATA.rawValue] as? String
            val isPenPalsConversation = data[SerializableKey.IS_PEN_PALS_CONVERSATION.rawValue] as? Boolean
            val lastModifiedString = data[SerializableKey.LAST_MODIFIED_DATE.rawValue] as? String
            val lastModifiedDate =
                lastModifiedString?.let {
                    DependencyValues.current.timestampDateFormatter.parse(it)
                }
            val encodedConsent = stringList(data, SerializableKey.MESSAGE_RECIPIENT_CONSENT_ACKNOWLEDGEMENT_DATA)
            val encodedSharing = stringList(data, SerializableKey.PEN_PALS_SHARING_DATA)
            val requiresConsent = data[SerializableKey.REQUIRES_CONSENT_FROM_INITIATOR.rawValue] as? String

            if (name == null ||
                imageDataString == null ||
                isPenPalsConversation == null ||
                lastModifiedDate == null ||
                encodedConsent == null ||
                encodedSharing == null ||
                requiresConsent == null
            ) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            val messageRecipientConsentAcknowledgementData =
                encodedConsent.map { MessageRecipientConsentAcknowledgementData.decode(it) }
            val penPalsSharingData = encodedSharing.map { PenPalsSharingData.decode(it) }

            if (messageRecipientConsentAcknowledgementData.isEmpty() ||
                penPalsSharingData.isEmpty() ||
                messageRecipientConsentAcknowledgementData.size != encodedConsent.size ||
                penPalsSharingData.size != encodedSharing.size
            ) {
                throw Exception(
                    "Mismatched ratio returned.",
                    metadata = ExceptionMetadata(this),
                )
            }

            val imageData =
                if (imageDataString.isBangQualifiedEmpty) {
                    null
                } else {
                    decodeBase64(imageDataString) ?: throw Exception.Networking.decodingFailed(
                        data,
                        ExceptionMetadata(this),
                    )
                }

            return ConversationMetadata(
                name = name,
                imageData = imageData,
                imageHash = data[SerializableKey.IMAGE_HASH.rawValue] as? String,
                isPenPalsConversation = isPenPalsConversation,
                lastModifiedDate = lastModifiedDate,
                messageRecipientConsentAcknowledgementData = messageRecipientConsentAcknowledgementData,
                penPalsSharingData = penPalsSharingData,
                requiresConsentFromInitiator = if (requiresConsent.isBangQualifiedEmpty) null else requiresConsent,
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun stringList(
            data: Map<String, Any?>,
            key: SerializableKey,
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
