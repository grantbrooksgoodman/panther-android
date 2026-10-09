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
import us.neotechnica.panther.bundle.users
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.common.withGlobalCacheStrategy
import us.neotechnica.panther.modules.networking.user.models.DeviceID
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.models.UserDataSnapshot
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.database.interfaces.getValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Coalescer
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.SingleSlotCoalescer
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import kotlin.time.Duration.Companion.milliseconds

/**
 * The service that creates and retrieves users.
 *
 * `UserService` creates new user records, fetches users by identifier
 * or phone number, and caches recently-fetched user data in
 * short-lived snapshots. Every user it returns is upserted into the
 * session store.
 */
object UserService {
    // MARK: - Properties

    private val keyedCoalescer = Coalescer<String, User>()
    private val singleSlotCoalescer = SingleSlotCoalescer<List<User>>()

    private val cachedUserDataSnapshots = LockIsolated<List<UserDataSnapshot>?>(null)

    private val database get() = Networking.config.databaseDelegate

    // MARK: - User Creation

    /**
     * Creates a new user with the given properties and writes it to
     * the database.
     *
     * @param id The identifier for the new user.
     * @param languageCode The user's language code.
     * @param phoneNumber The user's phone number.
     * @param pushTokens The user's push notification tokens, or `null`
     *   if none.
     *
     * @return The created user.
     *
     * @throws Exception if an account already exists for the phone
     *   number, or if writing the user fails.
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

        val mockUser =
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

        val data = mockUser.encoded.filterKeys { it != User.SerializableKey.ID.rawValue }.toMutableMap()
        data[User.SerializableKey.BADGE_NUMBER.rawValue] = 0

        database.setValue(
            value = data,
            key = "${NetworkPath.users.rawValue}/$id",
        )

        return mockUser
    }

    // MARK: - Collision Detection

    /**
     * Returns a Boolean value that indicates whether an account is
     * registered with the given phone number.
     *
     * @param phoneNumber The phone number to check.
     *
     * @return `true` if an account exists for the phone number;
     *   otherwise, `false`.
     */
    suspend fun accountExists(phoneNumber: PhoneNumber): Boolean =
        try {
            getUser(phoneNumber)
            true
        } catch (_: Exception) {
            false
        }

    // MARK: - Get All Users

    /**
     * Returns every user in the database.
     *
     * Concurrent calls coalesce onto a single in-flight fetch.
     *
     * @return Every user.
     *
     * @throws Exception if the users cannot be fetched.
     */
    suspend fun getAllUsers(): List<User> = singleSlotCoalescer { fetchAllUsers() }

    private suspend fun fetchAllUsers(): List<User> {
        val userData: Map<String, Any?> = database.getValues<Map<String, Any?>>(NetworkPath.users.rawValue)

        // Decode every user from the snapshot already downloaded above,
        // rather than re-fetching each record individually by ID.
        val userDataByID =
            userData.mapNotNull { (id, value) ->
                @Suppress("UNCHECKED_CAST")
                (value as? Map<String, Any?>)?.toMutableMap()?.apply { put(User.SerializableKey.ID.rawValue, id) }
            }

        cachedUserDataSnapshots.wrappedValue =
            userDataByID.map {
                UserDataSnapshot(
                    data = it,
                    expiryThreshold = SNAPSHOT_EXPIRY_MILLIS.milliseconds,
                )
            }

        if (userDataByID.isEmpty()) {
            throw Exception(
                "No users to decode.",
                metadata = ExceptionMetadata(this),
            )
        }

        return coroutineScope {
            userDataByID.map { data -> async { user(data) } }.awaitAll()
        }
    }

    // MARK: - Retrieval by ID

    /**
     * Returns the user with the given identifier.
     *
     * Unless bypassed, an unexpired cached snapshot is returned instead
     * of re-fetching.
     *
     * @param id The identifier of the user to fetch.
     * @param bypassSnapshotCache A Boolean value that determines whether
     *   to ignore the snapshot cache and always fetch.
     * @param cacheStrategy The database cache strategy to apply for the
     *   fetch, or `null` to use the default.
     *
     * @return The user.
     *
     * @throws Exception if no identifier is provided or the user cannot
     *   be fetched or decoded.
     */
    suspend fun getUser(
        id: String,
        bypassSnapshotCache: Boolean = false,
        cacheStrategy: CacheStrategy? = null,
    ): User {
        val userInfo = mapOf<String, Any>("UserID" to id)

        if (id.isBangQualifiedEmpty) {
            throw Exception(
                "No ID provided.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        // Coalesce concurrent fetches of the same user so participants
        // shared across conversations resolve – and upsert – only once.
        return keyedCoalescer("$id|$bypassSnapshotCache|$cacheStrategy") {
            fetchUser(
                id = id,
                bypassSnapshotCache = bypassSnapshotCache,
                cacheStrategy = cacheStrategy,
            )
        }
    }

    private suspend fun fetchUser(
        id: String,
        bypassSnapshotCache: Boolean,
        cacheStrategy: CacheStrategy?,
    ): User {
        val userInfo = mapOf<String, Any>("UserID" to id)

        if (!bypassSnapshotCache) {
            val match =
                cachedUserDataSnapshots.wrappedValue?.firstOrNull {
                    (it.data[User.SerializableKey.ID.rawValue] as? String) == id
                }

            if (match != null && !match.isExpired) {
                Logger.log(
                    Exception(
                        "Returning cached user data snapshot.",
                        isReportable = false,
                        userInfo = mapOf("UserID" to id),
                        metadata = ExceptionMetadata(this),
                    ),
                    domain = LoggerDomain.caches,
                )

                try {
                    return user(match.data)
                } catch (exception: Exception) {
                    throw exception.appending(userInfo)
                }
            }
        }

        val path = "${NetworkPath.users.rawValue}/$id"
        val fetched: Map<String, Any?> =
            try {
                if (cacheStrategy != null) {
                    database.withGlobalCacheStrategy(cacheStrategy) { database.getValues<Map<String, Any?>>(path) }
                } else {
                    database.getValues<Map<String, Any?>>(path)
                }
            } catch (exception: Exception) {
                throw exception.appending(userInfo)
            }

        val data = fetched.toMutableMap().apply { put(User.SerializableKey.ID.rawValue, id) }

        cachedUserDataSnapshots.withValue {
            it.value =
                (it.value ?: emptyList()) +
                UserDataSnapshot(
                    data = data,
                    expiryThreshold = SNAPSHOT_EXPIRY_MILLIS.milliseconds,
                )
        }

        try {
            return user(data)
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }

    /**
     * Returns the users with the given identifiers, fetched
     * concurrently.
     *
     * @param ids The identifiers of the users to fetch.
     * @param bypassSnapshotCache A Boolean value that determines whether
     *   to ignore the snapshot cache and always fetch.
     * @param cacheStrategy The database cache strategy to apply for the
     *   fetches, or `null` to use the default.
     *
     * @return The users.
     *
     * @throws Exception if no identifiers are provided or any user
     *   cannot be fetched.
     */
    suspend fun getUsers(
        ids: List<String>,
        bypassSnapshotCache: Boolean = false,
        cacheStrategy: CacheStrategy? = null,
    ): List<User> {
        val userInfo = mapOf<String, Any>("UserIDs" to ids)

        if (ids.isBangQualifiedEmpty) {
            throw Exception(
                "No ID keys provided.",
                metadata = ExceptionMetadata(this),
            ).appending(userInfo)
        }

        try {
            return coroutineScope {
                ids
                    .map { id ->
                        async {
                            getUser(
                                id = id,
                                bypassSnapshotCache = bypassSnapshotCache,
                                cacheStrategy = cacheStrategy,
                            )
                        }
                    }.awaitAll()
            }
        } catch (exception: Exception) {
            throw exception.appending(userInfo)
        }
    }

    // MARK: - Retrieval by Phone Number

    /**
     * Returns the user registered with the given phone number.
     *
     * @param phoneNumber The phone number to match.
     *
     * @return The matching user.
     *
     * @throws Exception if no user is registered with the phone number,
     *   or if the users cannot be fetched.
     */
    suspend fun getUser(phoneNumber: PhoneNumber): User {
        val userInfo = mapOf<String, Any>("PhoneNumber" to phoneNumber.encoded)

        val users =
            try {
                getAllUsers()
            } catch (exception: Exception) {
                throw exception.appending(userInfo)
            }

        return users.firstOrNull {
            it.phoneNumber.compiledNumberString == phoneNumber.compiledNumberString
        } ?: throw Exception(
            "No users with the provided phone number.",
            isReportable = false,
            metadata = ExceptionMetadata(this),
        ).appending(userInfo)
    }

    // MARK: - Clear Cache

    /** Removes every cached user data snapshot. */
    fun clearCache() {
        cachedUserDataSnapshots.wrappedValue = null
    }

    // MARK: - Auxiliary

    private fun user(data: Map<String, Any?>): User {
        val user = User.decode(data)

        /* Single source of upsert for fetched users – guarantees any user
         this service returns is resolvable from the session store
         (e.g. NumberPair.users). Bypasses RemotelyUpdatable.update.
         */
        SessionStore.upsertUser(user)
        return user
    }

    // MARK: - Companion

    private const val SNAPSHOT_EXPIRY_MILLIS = 500L
}
