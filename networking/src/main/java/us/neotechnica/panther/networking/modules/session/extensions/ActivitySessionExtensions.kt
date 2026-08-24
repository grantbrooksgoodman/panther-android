//
//  ActivitySessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.common.models.CommonConstants
import us.neotechnica.panther.networking.modules.schema.conversation.models.Activity
import us.neotechnica.panther.networking.modules.schema.conversation.models.ActivityAction
import us.neotechnica.panther.networking.modules.schema.message.models.HostedContentType
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.message.models.TranslationReference
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference as HostedTranslationReference

/** Whether the action records the current user being added to a conversation. */
val ActivityAction.isCurrentUserAdded: Boolean
    get() = this is ActivityAction.AddedToConversation && userID == User.currentUserID

/**
 * A human-readable description of the activity, with participant names
 * wrapped in `⌘…⌘` sentinels so the system-message cell can bold them.
 *
 * **Note:** this Phase 7 port renders a fixed English description from
 * the session store; the iOS original resolves localized templates and
 * contact names (deferred with the localization/contact layers).
 */
val Activity.description: String
    get() {
        val actor = "⌘${displayName(userID)}⌘"
        return when (val action = action) {
            is ActivityAction.AddedToConversation ->
                "$actor added ⌘${displayName(action.userID)}⌘ to the conversation."
            ActivityAction.ChangedGroupPhoto -> "$actor changed the group photo."
            ActivityAction.LeftConversation -> "$actor left the conversation."
            is ActivityAction.RemovedFromConversation ->
                "$actor removed ⌘${displayName(action.userID)}⌘ from the conversation."
            ActivityAction.RemovedGroupPhoto -> "$actor removed the group photo."
            ActivityAction.RemovedName -> "$actor removed the conversation name."
            is ActivityAction.RenamedConversation ->
                "$actor named the conversation ⌘“${action.name}”⌘."
        }
    }

/** A system message that represents the activity in a conversation. */
val Activity.message: Message
    get() {
        val translation =
            Translation(
                input = TranslationInput(description),
                output = description,
                languagePair = LanguagePair.system,
            )

        return Message(
            id = encodedHash,
            fromAccountID = CommonConstants.SYSTEM_MESSAGE_ID,
            contentType = HostedContentType.Text,
            translationReferences = listOf(TranslationReference(HostedTranslationReference.from(translation).hostingKey)),
            readReceipts = null,
            sentDate = date,
            translations = listOf(translation),
        )
    }

private fun displayName(userID: String): String {
    if (userID == User.currentUserID) return "You"
    val user = SessionStore.users[userID] ?: return "Someone"
    return "+${user.phoneNumber.callingCode} ${user.phoneNumber.nationalNumberString}"
}
