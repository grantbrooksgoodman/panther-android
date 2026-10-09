//
//  NotificationExtensionConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.constants

/**
 * The persistent-storage key names and push payload keys shared
 * with the notification delivery path.
 *
 * The messaging service runs in-process, so the persisted values
 * are stored through the app's regular storage and read at cold
 * start to enrich notifications before the session store loads.
 */
object NotificationExtensionConstants {
    /** The key under which the contact-name archive is persisted. */
    const val CONTACT_ARCHIVE_DEFAULTS_KEY_NAME = "notificationExtensionContactArchive"

    /**
     * The key under which the group conversation name map is
     * persisted.
     */
    const val CONVERSATION_NAME_MAP_DEFAULTS_KEY_NAME = "notificationExtensionConversationNameMap"

    /** The push payload key for the conversation identifier key. */
    const val CONVERSATION_ID_KEY_USER_INFO_KEY = "conversationIDKey"

    /** The push payload key for the reacted-to message identifier. */
    const val REACTION_MESSAGE_ID_USER_INFO_KEY = "reactionMessageID"

    /** The push payload key for the reaction title suffix. */
    const val REACTION_SUFFIX_USER_INFO_KEY = "reactionSuffix"

    /** The push payload key for the sender's phone number hash. */
    const val USER_NUMBER_HASH_USER_INFO_KEY = "userNumberHash"
}
