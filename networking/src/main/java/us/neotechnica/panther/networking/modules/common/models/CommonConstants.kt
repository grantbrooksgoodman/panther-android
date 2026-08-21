//
//  CommonConstants.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.models

/**
 * Sentinel identifiers shared across the networking and session layers.
 *
 * Mirrors the iOS `CommonConstants` values used to mark placeholder
 * conversations and messages.
 */
object CommonConstants {
    /** The identifier of a not-yet-created conversation. */
    const val NEW_CONVERSATION_ID = "EMPTY"

    /** The identifier of a not-yet-sent (mock) message. */
    const val NEW_MESSAGE_ID = "NEW"

    /** The account identifier used for system messages. */
    const val SYSTEM_MESSAGE_ID = "SYSTEM"
}
