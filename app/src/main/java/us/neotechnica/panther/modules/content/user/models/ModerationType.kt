//
//  ModerationType.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

/**
 * A kind of user moderation action.
 */
enum class ModerationType(
    val rawValue: String,
) {
    /** The action that blocks a user. */
    BLOCK("block"),

    /** The action that reports a user. */
    REPORT("report"),

    /** The action that unblocks a user. */
    UNBLOCK("unblock"),
    ;

    // MARK: - Computed Properties

    /** The confirmation message shown before applying the action to all users in a conversation. */
    val allUsersConfirmationMessage: String
        get() =
            when (this) {
                BLOCK ->
                    "Are you sure you'd like to block all users in this conversation?\n\n" +
                        "You will no longer receive messages from any chat in which you and any of these " +
                        "users are participants. This can be changed later in Settings.\n\n" +
                        "The other users will not know you have blocked them."
                REPORT -> "Are you sure you'd like to report all users in this conversation for improper conduct?"
                UNBLOCK -> ""
            }

    /** The confirmation message shown before applying the action to a single user. */
    val singleUserConfirmationMessage: String
        get() =
            when (this) {
                BLOCK ->
                    "Are you sure you'd like to block this user?\n\n" +
                        "You will no longer receive messages from any chat in which you and this " +
                        "user are participants. This can be changed later in Settings.\n\n" +
                        "The other user will not know you have blocked them."
                REPORT -> "Are you sure you'd like to report this user for improper conduct?"
                UNBLOCK -> ""
            }
}
