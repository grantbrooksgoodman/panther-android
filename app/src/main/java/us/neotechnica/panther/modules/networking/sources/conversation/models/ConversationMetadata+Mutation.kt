//
//  ConversationMetadata+Mutation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.conversation.models

import us.neotechnica.panther.modules.networking.sources.common.extensions.stringList
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.TimestampDateFormatter
import java.security.MessageDigest
import java.util.Base64
import java.util.Date

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
 * @param name The new name, or `null` to keep the current
 *   name.
 * @param imageData The new image data, or an empty value to
 *   keep the current image data.
 * @param isPenPalsConversation The new value, or `null` to
 *   keep the current value.
 * @param lastModifiedDate The new last-modified date, or
 *   `null` to keep the current date.
 * @param messageRecipientConsentAcknowledgementData The new
 *   consent acknowledgement records, or `null` to keep the
 *   current records.
 * @param penPalsSharingData The new PenPals sharing records,
 *   or `null` to keep the current records.
 * @param requiresConsentFromInitiator The new value, or an
 *   empty string to keep the current value.
 * @param nilImageData A Boolean value that, when `true`,
 *   clears the image data.
 * @param nilRequiresConsentFromInitiator A Boolean value
 *   that, when `true`, clears the required-consent value.
 *
 * @return The updated metadata.
 */
@Suppress("LongParameterList")
fun ConversationMetadata.copyWith(
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
