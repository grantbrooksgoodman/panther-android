//
//  NotificationExtensionConstants.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 27/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.constants

/**
 * The persistent-storage key names shared with the notification
 * delivery path.
 *
 * On iOS these name entries in the app-group `UserDefaults` suite read
 * by the notification service extension. Android has no notification
 * extension – the messaging service runs in-process – so the values
 * are persisted through the app's regular storage and read at cold
 * start to resolve notification subtitles before the session store
 * loads.
 */
object NotificationExtensionConstants {
    /**
     * The key under which the group conversation name map is
     * persisted.
     */
    const val CONVERSATION_NAME_MAP_KEY = "conversationNameMap"
}
