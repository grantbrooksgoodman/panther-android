//
//  ActivityAction+SessionExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.entity.extensions

import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.networking.conversation.models.Activity
import us.neotechnica.panther.modules.networking.conversation.models.ActivityAction
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/** Whether the action records the current user being added to a conversation. */
val ActivityAction.isCurrentUserAdded: Boolean
    get() = this is ActivityAction.AddedToConversation && userID == User.currentUserID

/**
 * A human-readable description of the activity, with participant names
 * wrapped in `⌘…⌘` sentinels so the system-message cell can bold them.
 *
 * Each participant name resolves to the contact's name when known,
 * falling back to the participant's formatted phone number.
 *
 * **Note:** this renders a fixed English description; localized
 * templates are not yet resolved.
 */
val Activity.description: String
    get() {
        val actor = "⌘${displayName(userID)}⌘"
        return when (val action = action) {
            is ActivityAction.AddedToConversation ->
                "$actor added ⌘${displayName(action.userID)}⌘ to the conversation."
            ActivityAction.ChangedGroupPhoto -> "$actor changed the group photo."
            ActivityAction.LeftConversation ->
                "⌘${displayNameForPhoneNumberString(userID)}⌘ left the conversation."
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
            richContent = null,
            translationReferences = listOf(translation.reference),
            translations = listOf(translation),
            readReceipts = null,
            sentDate = date,
        )
    }

private fun displayName(userID: String): String {
    if (userID == User.currentUserID) return "You"
    val user = SessionStore.users[userID] ?: return "Someone"
    return user.displayName
}

/**
 * Resolves the display name for a participant stored as a compiled
 * phone number string, as the `leftConversation` activity records it.
 *
 * Strings that contain letters are treated as user identifiers for
 * backward compatibility and resolved through [displayName]. Otherwise,
 * the string is matched against the current user and the session store
 * by compiled number, falling back to the formatted phone number.
 */
private fun displayNameForPhoneNumberString(phoneNumberString: String): String {
    // Backward compatibility. Remove in a future update.
    if (phoneNumberString.any { it.isLetter() }) return displayName(phoneNumberString)

    val currentUserNumberString = UserSessionService.currentUser?.phoneNumber?.compiledNumberString
    if (phoneNumberString == currentUserNumberString) return "You"

    val user = SessionStore.users.values.firstOrNull { it.phoneNumber.compiledNumberString == phoneNumberString }
    return user?.displayName ?: PhoneNumber(phoneNumberString).formattedString()
}
