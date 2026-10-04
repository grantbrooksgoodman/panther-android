//
//  User+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.common.contacts.services.ContactPairArchiveService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.services.PushTokenService
import us.neotechnica.panther.modules.networking.user.models.DeviceID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.UserUpdatableKey
import us.neotechnica.panther.modules.networking.user.remotelyupdatable.update
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

// MARK: - Properties

/** The contact pair matching this user in the contact pair archive, if one exists. */
val User.contactPair: ContactPair?
    get() = ContactPairArchiveService.getValue(phoneNumber)

/**
 * The user's display name.
 *
 * Resolves to the user's contact's full name when known, or their
 * formatted phone number otherwise. Results are cached in memory per
 * user.
 */
val User.displayName: String
    get() {
        UserDisplayNameCache.cachedValue(id)?.let { return it }

        val contactPairName = contactPair?.contact?.fullName
        if (!contactPairName.isNullOrBlank()) {
            UserDisplayNameCache.cache(id, contactPairName)
            return contactPairName
        }

        val formattedPhoneNumberString = phoneNumber.formattedString()
        UserDisplayNameCache.cache(id, formattedPhoneNumberString)
        return formattedPhoneNumberString
    }

// MARK: - Methods

/**
 * Removes the device's current push token from the user's push
 * tokens.
 *
 * This method has no effect when no current push token is available.
 *
 * @throws Exception if updating the user fails.
 */
suspend fun User.removeCurrentPushToken() {
    val currentPushToken = PushTokenService.currentToken ?: return

    var filteredPushTokens = (pushTokens ?: emptyList()).filter { it != currentPushToken }
    if (filteredPushTokens.isBangQualifiedEmpty) {
        filteredPushTokens = bangQualifiedEmptyList
    }

    update(UserUpdatableKey.PUSH_TOKENS, to = filteredPushTokens)
}

/**
 * Updates the user's device identifier to the current device's, if it
 * differs.
 *
 * Claiming the current device's identifier keeps the single-active-device
 * observer from signing this device out against a node device identifier
 * last written by another device.
 *
 * @throws Exception if updating the user fails.
 */
suspend fun User.updateDeviceIDIfNeeded() {
    val currentDeviceID = DeviceID.current
    if (deviceID == currentDeviceID) return
    update(UserUpdatableKey.DEVICE_ID, to = currentDeviceID)
}

// MARK: - User Display Name Cache

/** Manages the in-memory user display name cache. */
object UserDisplayNameCache {
    // MARK: - Properties

    private val cachedDisplayNamesForUserIDs = LockIsolated<Map<String, String>?>(null)

    // MARK: - Methods

    /** Removes every cached user display name. */
    fun clearCache() {
        cachedDisplayNamesForUserIDs.wrappedValue = null
    }

    /**
     * Removes the cached display names for the given user identifiers.
     *
     * @param ids The identifiers of the users whose cached display
     *   names to remove.
     */
    fun removeValues(forUserIDs: Set<String>) {
        val cache = cachedDisplayNamesForUserIDs.wrappedValue ?: return
        cachedDisplayNamesForUserIDs.wrappedValue = cache.filterKeys { it !in forUserIDs }
    }

    internal fun cache(
        id: String,
        displayName: String,
    ) {
        val cache = (cachedDisplayNamesForUserIDs.wrappedValue ?: emptyMap()).toMutableMap()
        cache[id] = displayName
        cachedDisplayNamesForUserIDs.wrappedValue = cache
    }

    internal fun cachedValue(id: String): String? = cachedDisplayNamesForUserIDs.wrappedValue?.get(id)
}
