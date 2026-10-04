//
//  NotificationExtensionConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.constants

/**
 * The persistent-storage key names shared with the notification
 * delivery path.
 *
 * The messaging service runs in-process (there is no separate
 * notification extension), so the values
 * are persisted through the app's regular storage and read at cold
 * start to resolve notification subtitles before the session store
 * loads.
 */
object NotificationExtensionConstants {
    /**
     * The key under which the number-hash to contact-name map is
     * persisted.
     */
    const val CONTACT_NAME_MAP_KEY = "contactNameMap"

    /**
     * The key under which the group conversation name map is
     * persisted.
     */
    const val CONVERSATION_NAME_MAP_KEY = "conversationNameMap"
}
