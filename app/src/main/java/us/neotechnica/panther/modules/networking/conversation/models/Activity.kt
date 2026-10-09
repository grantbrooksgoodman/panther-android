//
//  Activity.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.conversation.models

import us.neotechnica.panther.modules.common.constants.CommonConstants
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.message.models.HostedContentType
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.networking.modules.translation.extensions.system
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.Date

/**
 * A recorded change to a conversation, such as a participant
 * joining, leaving, or renaming it.
 */
data class Activity(
    /** The kind of change the activity records. */
    val action: Action,
    /** The date the change occurred. */
    val date: Date,
    /** The identifier of the user responsible for the change. */
    val userID: String,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Types

    /** A kind of change to a conversation that an [Activity] records. */
    sealed interface Action {
        // MARK: - Properties

        /** The string representation of the action. */
        val rawValue: String

        // MARK: - Cases

        /**
         * A user was added to the conversation, identified by the
         * associated user identifier.
         */
        data class AddedToConversation(
            val userID: String,
        ) : Action {
            override val rawValue: String get() = "ADDED:$userID"
        }

        /** The conversation's group photo was changed. */
        data object ChangedGroupPhoto : Action {
            override val rawValue: String get() = "CHANGED_PHOTO"
        }

        /** A user left the conversation. */
        data object LeftConversation : Action {
            override val rawValue: String get() = "LEFT"
        }

        /**
         * A user was removed from the conversation, identified by the
         * associated user identifier.
         */
        data class RemovedFromConversation(
            val userID: String,
        ) : Action {
            override val rawValue: String get() = "REMOVED:$userID"
        }

        /** The conversation's group photo was removed. */
        data object RemovedGroupPhoto : Action {
            override val rawValue: String get() = "REMOVED_PHOTO"
        }

        /** The conversation's name was removed. */
        data object RemovedName : Action {
            override val rawValue: String get() = "REMOVED_NAME"
        }

        /** The conversation was renamed, carrying the associated new name. */
        data class RenamedConversation(
            val name: String,
        ) : Action {
            override val rawValue: String get() = "RENAMED:$name"
        }

        // MARK: - Companion

        companion object {
            /**
             * Creates an action from its string representation.
             *
             * @param rawValue The string representation of the action.
             *
             * @return The action, or `null` if the string does not
             *   represent a known action.
             */
            fun from(rawValue: String): Action? {
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

    /** The serializable keys for encoding and decoding an activity. */
    enum class SerializableKey(
        val rawValue: String,
    ) {
        ACTION("action"),
        DATE("date"),
        USER_ID("userID"),
    }

    // MARK: - Computed Properties

    /**
     * A localized description of the activity, suitable for display.
     *
     * The description is resolved once per activity version and
     * cached in memory.
     */
    val description: String
        get() {
            ActivityDescriptionCache.description(encodedHash)?.let { return it }

            val localizedString =
                when (val action = action) {
                    is Action.AddedToConversation ->
                        LocalizedStringKey.AddedToConversation
                            .localized()
                            .replace("⌘", "⌘${userDisplayName(userID)}⌘")
                            .replace("⁂", "⌘${otherUserDisplayName(action.userID)}⌘")

                    Action.ChangedGroupPhoto ->
                        LocalizedStringKey.ChangedGroupPhoto
                            .localized()
                            .replace("⌘", "⌘${userDisplayName(userID)}⌘")

                    Action.LeftConversation ->
                        LocalizedStringKey.LeftConversation
                            .localized()
                            .replace("⌘", "⌘${phoneNumberDisplayName(userID)}⌘")

                    is Action.RemovedFromConversation ->
                        LocalizedStringKey.RemovedFromConversation
                            .localized()
                            .replace("⌘", "⌘${userDisplayName(userID)}⌘")
                            .replace("⁂", "⌘${otherUserDisplayName(action.userID)}⌘")

                    Action.RemovedGroupPhoto ->
                        LocalizedStringKey.RemovedGroupPhoto
                            .localized()
                            .replace("⌘", "⌘${userDisplayName(userID)}⌘")

                    Action.RemovedName ->
                        LocalizedStringKey.RemovedConversationName
                            .localized()
                            .replace("⌘", "⌘${userDisplayName(userID)}⌘")

                    is Action.RenamedConversation ->
                        LocalizedStringKey.RenamedConversation
                            .localized()
                            .replace("⌘", "⌘${userDisplayName(userID)}⌘")
                            .replace("⁂", "⌘“${action.name}”⌘")
                }

            ActivityDescriptionCache.setDescription(localizedString, encodedHash)
            return localizedString
        }

    /** The serialized representation of the activity. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                SerializableKey.ACTION.rawValue to action.rawValue,
                SerializableKey.DATE.rawValue to DependencyValues.current.timestampDateFormatter.format(date),
                SerializableKey.USER_ID.rawValue to userID,
            )

    /**
     * The strings that collectively define this instance's identity
     * for hashing purposes, sorted alphabetically.
     */
    override val hashFactors: List<String>
        get() =
            listOf(
                action.rawValue,
                DependencyValues.current.timestampDateFormatter.format(date),
                userID,
            ).sorted()

    /** A system message that represents the activity in a conversation. */
    val message: Message
        get() =
            Message(
                id = encodedHash,
                fromAccountID = CommonConstants.SYSTEM_MESSAGE_ID,
                contentType = HostedContentType.Text,
                richContent = null,
                translationReferences =
                    listOf(
                        TranslationReference(
                            languagePair = LanguagePair.system,
                            type = TranslationReference.Type.Idempotent(encodedHash),
                        ),
                    ),
                translations =
                    listOf(
                        Translation(
                            input = TranslationInput(encodedHash),
                            output = encodedHash,
                            languagePair = LanguagePair.system,
                        ),
                    ),
                readReceipts = null,
                sentDate = date,
            )

    // MARK: - Auxiliary

    private fun userDisplayName(userID: String): String {
        if (userID == User.currentUserID) return LocalizedStringKey.You.localized()
        val sessionStore = DependencyValues.current.clientSession.store
        return sessionStore.users[userID]?.displayName ?: LocalizedStringKey.Someone.localized()
    }

    private fun phoneNumberDisplayName(phoneNumberString: String): String {
        val clientSession = DependencyValues.current.clientSession

        // Backward compatibility. Remove in a future update.
        if (phoneNumberString.any { it.isLetter() }) return userDisplayName(phoneNumberString)

        val currentUser = clientSession.entity.user.currentUser
        if (phoneNumberString == currentUser?.phoneNumber?.compiledNumberString) {
            return LocalizedStringKey.You.localized()
        }

        return clientSession.store.users.values
            .firstOrNull { it.phoneNumber.compiledNumberString == phoneNumberString }
            ?.displayName ?: PhoneNumber(phoneNumberString).formattedString()
    }

    private fun otherUserDisplayName(userID: String): String {
        val otherUserDisplayName = userDisplayName(userID)
        return if (otherUserDisplayName.isSomeoneOrYou) otherUserDisplayName.lowercase() else otherUserDisplayName
    }

    private val String.isSomeoneOrYou: Boolean
        get() = this == LocalizedStringKey.Someone.localized() || this == LocalizedStringKey.You.localized()

    // MARK: - Companion

    companion object : SerializableDecoder<Activity, Map<String, Any?>> {
        /** An empty activity placeholder. */
        val empty =
            Activity(
                action = Action.LeftConversation,
                date = Date(0),
                userID = BANG_QUALIFIED_EMPTY,
            )

        /**
         * Creates an activity for the given action, attributed to the
         * current user and dated now.
         *
         * @param action The kind of change the activity records.
         *
         * @return The activity, or `null` if the current user is
         *   unavailable.
         */
        fun from(action: Action): Activity? {
            val currentUserID = User.currentUserID ?: return null
            return when (action) {
                Action.LeftConversation -> {
                    val currentUser = DependencyValues.current.clientSession.entity.user.currentUser ?: return null
                    Activity(
                        action = action,
                        date = Date(),
                        userID = currentUser.phoneNumber.compiledNumberString,
                    )
                }

                else ->
                    Activity(
                        action = action,
                        date = Date(),
                        userID = currentUserID,
                    )
            }
        }

        override fun canDecode(data: Map<String, Any?>): Boolean {
            val actionString = data[SerializableKey.ACTION.rawValue] as? String ?: return false
            val dateString = data[SerializableKey.DATE.rawValue] as? String ?: return false
            val userID = data[SerializableKey.USER_ID.rawValue] as? String ?: return false
            return Action.from(actionString) != null &&
                DependencyValues.current.timestampDateFormatter.parse(dateString) != null &&
                userID.isNotBlank()
        }

        override fun decode(data: Map<String, Any?>): Activity {
            val actionString = data[SerializableKey.ACTION.rawValue] as? String
            val action = actionString?.let { Action.from(it) }
            val dateString = data[SerializableKey.DATE.rawValue] as? String
            val date = dateString?.let { DependencyValues.current.timestampDateFormatter.parse(it) }
            val userID = data[SerializableKey.USER_ID.rawValue] as? String

            if (action == null || date == null || userID == null) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            return Activity(
                action = action,
                date = date,
                userID = userID,
            )
        }
    }
}

/** A namespace for managing the in-memory activity description cache. */
object ActivityDescriptionCache {
    // MARK: - Properties

    private val cachedDescriptionsForEncodedHashes = LockIsolated(mapOf<String, String>())

    // MARK: - Methods

    /** Removes every cached activity description. */
    fun clearCache() {
        cachedDescriptionsForEncodedHashes.wrappedValue = emptyMap()
    }

    internal fun description(encodedHash: String): String? = cachedDescriptionsForEncodedHashes.wrappedValue[encodedHash]

    internal fun setDescription(
        description: String,
        encodedHash: String,
    ) {
        cachedDescriptionsForEncodedHashes.withValue { it.value = it.value + (encodedHash to description) }
    }
}
