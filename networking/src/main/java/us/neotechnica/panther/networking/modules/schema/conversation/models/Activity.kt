//
//  Activity.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.conversation.models

import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import java.util.Date

/**
 * A record of a change to a conversation.
 */
data class Activity(
    /** The kind of change the activity records. */
    val action: ActivityAction,
    /** The date the change occurred. */
    val date: Date,
    /** The identifier of the user responsible for the change. */
    val userID: String,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        ACTION("action"),
        DATE("date"),
        USER_ID("userID"),
    }

    // MARK: - Computed Properties

    /** The serialized representation of the activity. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.ACTION.rawValue to action.rawValue,
                Keys.DATE.rawValue to DependencyValues.current.timestampDateFormatter.format(date),
                Keys.USER_ID.rawValue to userID,
            )

    override val hashFactors: List<String>
        get() =
            listOf(
                action.rawValue,
                DependencyValues.current.timestampDateFormatter.format(date),
                userID,
            ).sorted()

    // MARK: - Companion

    companion object : SerializableDecoder<Activity, Map<String, Any?>> {
        /** An empty activity placeholder. */
        val empty =
            Activity(
                action = ActivityAction.LeftConversation,
                date = Date(0),
                userID = BANG_QUALIFIED_EMPTY,
            )

        override fun canDecode(data: Map<String, Any?>): Boolean {
            val actionString = data[Keys.ACTION.rawValue] as? String ?: return false
            val dateString = data[Keys.DATE.rawValue] as? String ?: return false
            val userID = data[Keys.USER_ID.rawValue] as? String ?: return false
            return ActivityAction.from(actionString) != null &&
                DependencyValues.current.timestampDateFormatter.parse(dateString) != null &&
                userID.isNotBlank()
        }

        override suspend fun decode(data: Map<String, Any?>): Activity {
            val actionString = data[Keys.ACTION.rawValue] as? String
            val action = actionString?.let { ActivityAction.from(it) }
            val dateString = data[Keys.DATE.rawValue] as? String
            val date = dateString?.let { DependencyValues.current.timestampDateFormatter.parse(it) }
            val userID = data[Keys.USER_ID.rawValue] as? String

            if (action == null || date == null || userID == null) {
                throw decodingFailure(this, data)
            }

            return Activity(
                action = action,
                date = date,
                userID = userID,
            )
        }
    }
}
