//
//  UserService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.user.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.user.models.DeviceID
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.KeyedCoalescer

/**
 * Creates and looks up [User] records in the database.
 *
 * Ported from the iOS `UserService`; for Phase 5 this covers account
 * creation and phone-number collision detection. Read paths for the
 * session layer arrive in a later phase.
 */
object UserService {
    // MARK: - Properties

    private val allUsersCoalescer = KeyedCoalescer<String, List<User>>()

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Create User

    /**
     * Creates a user record from the onboarding values, writing it to
     * `users/<id>`.
     *
     * @throws Exception if an account already exists for the phone
     *   number, or the write fails.
     */
    suspend fun createUser(
        id: String,
        languageCode: String,
        phoneNumber: PhoneNumber,
        pushTokens: List<String>?,
    ): User {
        if (accountExists(phoneNumber)) {
            throw Exception(
                "User already exists for this phone number.",
                userInfo = mapOf("PhoneNumber" to phoneNumber.encoded),
                metadata = ExceptionMetadata(this),
            )
        }

        val user =
            User(
                id = id,
                aiEnhancedTranslationsEnabled = false,
                blockedUserIDs = null,
                conversationIDs = null,
                deviceID = DeviceID.current,
                isPenPalsParticipant = false,
                languageCode = languageCode,
                messageRecipientConsentRequired = false,
                phoneNumber = phoneNumber,
                previousLanguageCodes = null,
                pushTokens = pushTokens,
            )

        val data =
            user.encoded
                .filterKeys { it != ID_KEY }
                .toMutableMap()
        data[BADGE_NUMBER_KEY] = 0

        database.setValue(
            value = data,
            key = "${NetworkPath.users.rawValue}/$id",
        )

        return user
    }

    // MARK: - Collision Detection

    /** Returns whether an account is registered for the phone number. */
    suspend fun accountExists(phoneNumber: PhoneNumber): Boolean =
        try {
            getUser(phoneNumber)
            true
        } catch (_: Exception) {
            false
        }

    // MARK: - Retrieval

    /** Returns the user registered with the given phone number. */
    suspend fun getUser(phoneNumber: PhoneNumber): User {
        val users = getAllUsers()
        return users.firstOrNull {
            it.phoneNumber.compiledNumberString == phoneNumber.compiledNumberString
        } ?: throw Exception(
            "No users with the provided phone number.",
            userInfo = mapOf("PhoneNumber" to phoneNumber.encoded),
            metadata = ExceptionMetadata(this),
        )
    }

    /** Returns every user in the database. Concurrent calls coalesce. */
    suspend fun getAllUsers(): List<User> = allUsersCoalescer(ALL_USERS_KEY) { fetchAllUsers() }

    // MARK: - Auxiliary

    private suspend fun fetchAllUsers(): List<User> {
        val usersNode: Map<String, Any?> = database.getValues(NetworkPath.users.rawValue)

        val users = mutableListOf<User>()
        for ((id, value) in usersNode) {
            @Suppress("UNCHECKED_CAST")
            val childData = (value as? Map<String, Any?>)?.toMutableMap() ?: continue
            childData[ID_KEY] = id
            if (User.canDecode(childData)) {
                users.add(User.decode(childData))
            }
        }
        return users
    }

    // MARK: - Companion

    private const val ID_KEY = "id"
    private const val BADGE_NUMBER_KEY = "badgeNumber"
    private const val ALL_USERS_KEY = "allUsers"
}
