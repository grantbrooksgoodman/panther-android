//
//  NumberPair.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable

/**
 * A phone number paired with the registered users it resolves to.
 */
class NumberPair(
    val phoneNumber: PhoneNumber,
    val userIDs: List<String>,
) : Serializable<Map<String, Any?>> {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        PHONE_NUMBER("phoneNumber"),
        USER_IDS("userIDs"),
    }

    // MARK: - Computed Properties

    /**
     * The users this number pair's [userIDs] resolve to in the session
     * store. Users not present in the store are omitted.
     */
    val users: List<User>
        get() = userIDs.mapNotNull { SessionStore.users[it] }

    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.PHONE_NUMBER.rawValue to phoneNumber.encoded,
                Keys.USER_IDS.rawValue to userIDs,
            )

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean =
        other is NumberPair && phoneNumber == other.phoneNumber && userIDs == other.userIDs

    override fun hashCode(): Int = 31 * phoneNumber.hashCode() + userIDs.hashCode()

    // MARK: - Companion

    companion object {
        /**
         * Reconstructs a number pair from its stored encoded map, or
         * `null` if the map is malformed.
         */
        fun decode(data: Map<String, Any?>): NumberPair? {
            val phoneNumber = PhoneNumber.decodeSynchronously(data[Keys.PHONE_NUMBER.rawValue]) ?: return null
            val userIDs = (data[Keys.USER_IDS.rawValue] as? List<*>)?.mapNotNull { it as? String } ?: return null
            return NumberPair(phoneNumber = phoneNumber, userIDs = userIDs)
        }
    }
}
