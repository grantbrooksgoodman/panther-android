//
//  ActivityAction.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

/**
 * The kind of change an [Activity] records.
 *
 * Each action serializes to a raw string. Parameterized actions
 * encode their payload after a `":"` separator (for example,
 * `"ADDED:<userID>"`).
 */
sealed interface ActivityAction {
    // MARK: - Properties

    /** The string representation of the action. */
    val rawValue: String

    // MARK: - Cases

    /** A user was added to the conversation. */
    data class AddedToConversation(
        val userID: String,
    ) : ActivityAction {
        override val rawValue: String get() = "ADDED:$userID"
    }

    /** The conversation's group photo was changed. */
    data object ChangedGroupPhoto : ActivityAction {
        override val rawValue: String get() = "CHANGED_PHOTO"
    }

    /** A user left the conversation. */
    data object LeftConversation : ActivityAction {
        override val rawValue: String get() = "LEFT"
    }

    /** A user was removed from the conversation. */
    data class RemovedFromConversation(
        val userID: String,
    ) : ActivityAction {
        override val rawValue: String get() = "REMOVED:$userID"
    }

    /** The conversation's group photo was removed. */
    data object RemovedGroupPhoto : ActivityAction {
        override val rawValue: String get() = "REMOVED_PHOTO"
    }

    /** The conversation's name was removed. */
    data object RemovedName : ActivityAction {
        override val rawValue: String get() = "REMOVED_NAME"
    }

    /** The conversation was renamed. */
    data class RenamedConversation(
        val name: String,
    ) : ActivityAction {
        override val rawValue: String get() = "RENAMED:$name"
    }

    // MARK: - Companion

    companion object {
        /**
         * Creates an action from its string representation, or
         * `null` if the string does not represent a known action.
         *
         * @param rawValue The string representation of the action.
         *
         * @return The action, or `null`.
         */
        fun from(rawValue: String): ActivityAction? {
            val components = rawValue.split(":")
            if (components.size != 2) {
                return when (rawValue) {
                    ChangedGroupPhoto.rawValue -> ChangedGroupPhoto
                    LeftConversation.rawValue -> LeftConversation
                    RemovedName.rawValue -> RemovedName
                    RemovedGroupPhoto.rawValue -> RemovedGroupPhoto
                    else -> null
                }
            }

            val suffix = components[1]
            return when (components[0]) {
                "ADDED" -> AddedToConversation(suffix)
                "REMOVED" -> RemovedFromConversation(suffix)
                "RENAMED" -> RenamedConversation(suffix)
                else -> null
            }
        }
    }
}
