//
//  UserService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.common.withGlobalCacheStrategy
import us.neotechnica.panther.modules.networking.user.models.DeviceID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.DataSample
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.KeyedCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

/**
 * Creates and retrieves [User] records in the database.
 *
 * Creates new user records, fetches users by identifier or phone
 * number, and caches recently-fetched user data in short-lived
 * snapshots. Every user it returns is upserted into the session
 * store.
 */
object UserService {
    // MARK: - Properties

    private val allUsersCoalescer = KeyedCoalescer<String, List<User>>()
    private val cachedUserDataSnapshots = LockIsolated<List<DataSample>?>(null)
    private val userCoalescer = KeyedCoalescer<String, User>()

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

    /** Returns every user in the database. Concurrent calls coalesce. */
    suspend fun getAllUsers(): List<User> = allUsersCoalescer(ALL_USERS_KEY) { fetchAllUsers() }

    /**
     * Returns the user with the given ID, upserting it into the
     * [SessionStore]. Concurrent fetches of the same ID coalesce.
     *
     * Unless bypassed, an unexpired cached snapshot is returned
     * instead of re-fetching.
     */
    suspend fun getUser(
        id: String,
        bypassSnapshotCache: Boolean = false,
        cacheStrategy: CacheStrategy? = null,
    ): User {
        if (id.isBangQualifiedEmpty) {
            throw Exception("No ID provided.", userInfo = mapOf("UserID" to id), metadata = ExceptionMetadata(this))
        }

        // Coalesce concurrent fetches of the same user so participants
        // shared across conversations resolve – and upsert – only once.
        return userCoalescer("$id|$bypassSnapshotCache|$cacheStrategy") {
            fetchUser(id, bypassSnapshotCache, cacheStrategy)
        }
    }

    /** Returns the users with the given IDs, upserting them into the store. */
    suspend fun getUsers(
        ids: List<String>,
        bypassSnapshotCache: Boolean = false,
        cacheStrategy: CacheStrategy? = null,
    ): List<User> {
        if (ids.isBangQualifiedEmpty) {
            throw Exception("No ID keys provided.", userInfo = mapOf("UserIDs" to ids.toString()), metadata = ExceptionMetadata(this))
        }

        // Fail the batch if any user cannot be fetched.
        return coroutineScope {
            ids.map { id -> async { getUser(id, bypassSnapshotCache, cacheStrategy) } }.awaitAll()
        }
    }

    /** Returns the user registered with the given phone number. */
    suspend fun getUser(phoneNumber: PhoneNumber): User {
        val users = getAllUsers()
        return users.firstOrNull {
            it.phoneNumber.compiledNumberString == phoneNumber.compiledNumberString
        } ?: throw Exception(
            "No users with the provided phone number.",
            isReportable = false,
            userInfo = mapOf("PhoneNumber" to phoneNumber.encoded),
            metadata = ExceptionMetadata(this),
        )
    }

    // MARK: - Clear Cache

    /** Removes every cached user data snapshot. */
    fun clearCache() {
        cachedUserDataSnapshots.wrappedValue = null
    }

    // MARK: - Auxiliary

    private suspend fun fetchAllUsers(): List<User> {
        val usersNode: Map<String, Any?> = database.getValues(NetworkPath.users.rawValue)

        // Decode every user from the snapshot already downloaded above,
        // rather than re-fetching each record individually by ID.
        val userDataByID =
            usersNode.mapNotNull { (id, value) ->
                @Suppress("UNCHECKED_CAST")
                (value as? Map<String, Any?>)?.toMutableMap()?.apply { put(ID_KEY, id) }
            }

        cachedUserDataSnapshots.wrappedValue = userDataByID.map { DataSample(it, SNAPSHOT_EXPIRY_MILLIS) }

        return coroutineScope {
            userDataByID.map { data -> async { userFrom(data) } }.awaitAll()
        }
    }

    private suspend fun fetchUser(
        id: String,
        bypassSnapshotCache: Boolean,
        cacheStrategy: CacheStrategy?,
    ): User {
        if (!bypassSnapshotCache) {
            val match =
                cachedUserDataSnapshots.wrappedValue?.firstOrNull {
                    (it.data as? Map<*, *>)?.get(ID_KEY) == id
                }
            if (match != null && !match.isExpired) {
                Logger.log("Returning cached user data snapshot.")
                @Suppress("UNCHECKED_CAST")
                return userFrom(match.data as Map<String, Any?>)
            }
        }

        val path = "${NetworkPath.users.rawValue}/$id"
        val fetched: Map<String, Any?> =
            if (cacheStrategy != null) {
                database.withGlobalCacheStrategy(cacheStrategy) { database.getValues(path) }
            } else {
                database.getValues(path)
            }

        val data = fetched.toMutableMap().apply { put(ID_KEY, id) }
        cachedUserDataSnapshots.withValue { it.value = (it.value ?: emptyList()) + DataSample(data, SNAPSHOT_EXPIRY_MILLIS) }
        return userFrom(data)
    }

    private suspend fun userFrom(data: Map<String, Any?>): User {
        if (!User.canDecode(data)) {
            throw Exception("Failed to decode user.", metadata = ExceptionMetadata(this))
        }
        return User.decode(data).also { SessionStore.upsertUser(it) }
    }

    // MARK: - Companion

    private const val ALL_USERS_KEY = "allUsers"
    private const val BADGE_NUMBER_KEY = "badgeNumber"
    private const val ID_KEY = "id"
    private const val SNAPSHOT_EXPIRY_MILLIS = 500L
}
