//
//  MessageSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.translation.services.TranslationResolver
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/** Whether the message was sent by the current user. */
val Message.isFromCurrentUser: Boolean
    get() = fromAccountID == User.currentUserID

/** Whether the current user has read the message. */
val Message.isReadByCurrentUser: Boolean
    get() = readReceipts?.any { it.userID == User.currentUserID } == true

/**
 * The message's text resolved into [languageCode], for previews.
 *
 * Chooses the translation targeting [languageCode], falling back to
 * the first available reference, and returns its output. Returns an
 * empty string when the message carries no resolvable text.
 */
suspend fun Message.resolvedText(languageCode: String): String {
    val parsed =
        translationReferences
            .orEmpty()
            .mapNotNull { HostedTranslationReference.fromString(it.hostingKey) }
    val reference =
        parsed.firstOrNull { it.languagePair.to == languageCode }
            ?: parsed.firstOrNull()
            ?: return ""

    return runCatching { TranslationResolver.resolve(reference).output }.getOrDefault("")
}
