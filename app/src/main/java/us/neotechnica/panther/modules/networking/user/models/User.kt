//
//  User.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.models

import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.currentUserID
import us.neotechnica.panther.modules.content.user.extensions.isFromCurrentUser
import us.neotechnica.panther.modules.networking.common.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.conversation.models.ConversationID
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * A registered user.
 *
 * A user carries their identity, phone number, language, and
 * preferences, along with the identifiers of their conversations,
 * blocked users, and push tokens. Values may be updated individually
 * and written to the remote database.
 *
 * **Note:** `badgeNumber` is not part of the serialized user; it
 * is seeded at creation and written out-of-band at
 * `users/<id>/badgeNumber`.
 */
data class User(
    /** The user's unique identifier. */
    val id: String,
    /** A Boolean value that indicates whether the user has enabled AI-enhanced translations. */
    val aiEnhancedTranslationsEnabled: Boolean,
    /** The identifiers of the users this user has blocked, or `null` if none. */
    val blockedUserIDs: List<String>?,
    /** The identifiers of the user's conversations, or `null` if none. */
    val conversationIDs: List<ConversationID>?,
    /** The identifier of the user's device. */
    val deviceID: String,
    /** A Boolean value that indicates whether the user participates in PenPals. */
    val isPenPalsParticipant: Boolean,
    /** The user's language code. */
    val languageCode: String,
    /** A Boolean value that indicates whether the user requires message-receipt consent. */
    val messageRecipientConsentRequired: Boolean,
    /** The user's phone number. */
    val phoneNumber: PhoneNumber,
    /** The user's previously-used language codes, or `null` if none. */
    val previousLanguageCodes: List<String>?,
    /** The user's push notification tokens, or `null` if none. */
    val pushTokens: List<String>?,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Types

    /** A category of session data associated with a user. */
    enum class DataType {
        /** The user's conversations. */
        CONVERSATIONS,

        /** The messages in the user's conversations. */
        MESSAGES,

        /** The other participants in the user's conversations. */
        USERS,
    }

    /** The serializable keys for encoding and decoding a user. */
    enum class SerializableKey(
        val rawValue: String,
    ) {
        ID("id"),
        AI_ENHANCED_TRANSLATIONS_ENABLED("aiEnhancedTranslationsEnabled"),
        BADGE_NUMBER("badgeNumber"),
        BLOCKED_USER_IDS("blockedUserIDs"),
        CONVERSATION_IDS("openConversations"),
        DEVICE_ID("deviceID"),
        IS_PEN_PALS_PARTICIPANT("isPenPalsParticipant"),
        LANGUAGE_CODE("languageCode"),
        MESSAGE_RECIPIENT_CONSENT_REQUIRED("messageRecipientConsentRequired"),
        PHONE_NUMBER("phoneNumber"),
        PREVIOUS_LANGUAGE_CODES("previousLanguageCodes"),
        PUSH_TOKENS("pushTokens"),
    }

    // MARK: - Computed Properties

    /**
     * Resolves conversations from the session store using this user's
     * `conversationIDs`.
     *
     * Returns `null` if any conversation is not in the store.
     */
    val conversations: List<Conversation>?
        get() {
            val sessionStore = DependencyValues.current.clientSession.store
            val conversationIDs = conversationIDs ?: return null
            val conversations = conversationIDs.mapNotNull { sessionStore.conversations[it.key] }
            if (conversations.size != conversationIDs.size) return null
            return conversations.ifEmpty { null }
        }

    /** The serialized representation of the user. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                SerializableKey.ID.rawValue to id,
                SerializableKey.AI_ENHANCED_TRANSLATIONS_ENABLED.rawValue to aiEnhancedTranslationsEnabled,
                SerializableKey.BLOCKED_USER_IDS.rawValue to
                    (blockedUserIDs?.takeIf { it.isNotEmpty() }?.associateWith { true } ?: emptyMap()),
                SerializableKey.CONVERSATION_IDS.rawValue to
                    (
                        conversationIDs
                            ?.takeIf { it.isNotEmpty() }
                            ?.associate { it.key to it.hash } ?: emptyMap<String, String>()
                    ),
                SerializableKey.DEVICE_ID.rawValue to deviceID,
                SerializableKey.IS_PEN_PALS_PARTICIPANT.rawValue to isPenPalsParticipant,
                SerializableKey.LANGUAGE_CODE.rawValue to languageCode,
                SerializableKey.MESSAGE_RECIPIENT_CONSENT_REQUIRED.rawValue to messageRecipientConsentRequired,
                SerializableKey.PHONE_NUMBER.rawValue to phoneNumber.encoded,
                SerializableKey.PREVIOUS_LANGUAGE_CODES.rawValue to (previousLanguageCodes ?: bangQualifiedEmptyList),
                SerializableKey.PUSH_TOKENS.rawValue to
                    (pushTokens?.takeIf { it.isNotEmpty() }?.associateWith { true } ?: emptyMap()),
            )

    /**
     * The strings that collectively define this instance's identity
     * for hashing purposes, sorted alphabetically.
     */
    override val hashFactors: List<String>
        get() =
            buildList {
                add(aiEnhancedTranslationsEnabled.toString())
                addAll(blockedUserIDs ?: emptyList())
                addAll(conversationIDs?.map { it.encoded } ?: emptyList())
                add(deviceID)
                add(isPenPalsParticipant.toString())
                add(languageCode)
                add(messageRecipientConsentRequired.toString())
                add(phoneNumber.encodedHash)
                addAll(previousLanguageCodes ?: emptyList())
                addAll(pushTokens ?: emptyList())
            }.sorted()

    // MARK: - Badge Number Calculation

    /**
     * Returns the number of unread incoming messages across the user's
     * visible conversations.
     *
     * @return The unread message count, or `0` for users other than the
     *   current user.
     */
    fun calculateBadgeNumber(): Int {
        if (id != User.currentUserID) return 0
        val conversations = conversations ?: return 0
        return conversations
            .visibleForCurrentUser
            .flatMap { it.messages ?: emptyList() }
            .count { !it.isFromCurrentUser && it.currentUserReadReceipt == null }
    }

    // MARK: - Companion

    companion object : SerializableDecoder<User, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[SerializableKey.ID.rawValue] !is String) return false
            if (data[SerializableKey.AI_ENHANCED_TRANSLATIONS_ENABLED.rawValue] !is Boolean) return false
            if (!isAbsentOrMap(data, SerializableKey.BLOCKED_USER_IDS)) return false
            if (!isAbsentOrMap(data, SerializableKey.CONVERSATION_IDS)) return false
            if (data[SerializableKey.DEVICE_ID.rawValue] !is String) return false
            if (data[SerializableKey.IS_PEN_PALS_PARTICIPANT.rawValue] !is Boolean) return false
            if (data[SerializableKey.MESSAGE_RECIPIENT_CONSENT_REQUIRED.rawValue] !is Boolean) return false
            val phoneNumber = mapValue(data, SerializableKey.PHONE_NUMBER) ?: return false
            if (!PhoneNumber.canDecode(phoneNumber)) return false
            if (data[SerializableKey.LANGUAGE_CODE.rawValue] !is String) return false
            if (stringList(data, SerializableKey.PREVIOUS_LANGUAGE_CODES) == null) return false
            return isAbsentOrMap(data, SerializableKey.PUSH_TOKENS)
        }

        override fun decode(data: Map<String, Any?>): User {
            val id = data[SerializableKey.ID.rawValue] as? String
            val aiEnhanced = data[SerializableKey.AI_ENHANCED_TRANSLATIONS_ENABLED.rawValue] as? Boolean
            val deviceID = data[SerializableKey.DEVICE_ID.rawValue] as? String
            val encodedPhoneNumber = mapValue(data, SerializableKey.PHONE_NUMBER)
            val isPenPalsParticipant = data[SerializableKey.IS_PEN_PALS_PARTICIPANT.rawValue] as? Boolean
            val languageCode = data[SerializableKey.LANGUAGE_CODE.rawValue] as? String
            val consentRequired = data[SerializableKey.MESSAGE_RECIPIENT_CONSENT_REQUIRED.rawValue] as? Boolean
            val previousLanguageCodes = stringList(data, SerializableKey.PREVIOUS_LANGUAGE_CODES)

            if (id == null ||
                aiEnhanced == null ||
                deviceID == null ||
                encodedPhoneNumber == null ||
                isPenPalsParticipant == null ||
                languageCode == null ||
                consentRequired == null ||
                previousLanguageCodes == null
            ) {
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
            }

            // Dictionaries carry no order; sort map-derived arrays so
            // re-decodes of identical data compare equal.
            val blockedUserIDs = stringKeyMap(data, SerializableKey.BLOCKED_USER_IDS)?.keys?.sorted() ?: emptyList()
            val conversationIDs =
                stringKeyMap(data, SerializableKey.CONVERSATION_IDS)
                    ?.map { (key, value) -> ConversationID(key = key, hash = value.toString()) }
                    ?.sortedBy { it.key }
                    ?: emptyList()
            val pushTokens = stringKeyMap(data, SerializableKey.PUSH_TOKENS)?.keys?.sorted() ?: emptyList()

            return User(
                id = id,
                aiEnhancedTranslationsEnabled = aiEnhanced,
                blockedUserIDs = if (blockedUserIDs.isBangQualifiedEmpty) null else blockedUserIDs,
                conversationIDs = conversationIDs.ifEmpty { null },
                deviceID = deviceID,
                isPenPalsParticipant = isPenPalsParticipant,
                languageCode = languageCode,
                messageRecipientConsentRequired = consentRequired,
                phoneNumber = PhoneNumber.decode(encodedPhoneNumber),
                previousLanguageCodes =
                    if (previousLanguageCodes.isBangQualifiedEmpty) {
                        null
                    } else {
                        previousLanguageCodes
                    },
                pushTokens = if (pushTokens.isBangQualifiedEmpty) null else pushTokens,
            )
        }

        private fun isAbsentOrMap(
            data: Map<String, Any?>,
            key: SerializableKey,
        ): Boolean {
            val value = data[key.rawValue] ?: return true
            return value is Map<*, *>
        }

        @Suppress("UNCHECKED_CAST")
        private fun mapValue(
            data: Map<String, Any?>,
            key: SerializableKey,
        ): Map<String, Any?>? = (data[key.rawValue] as? Map<*, *>)?.let { it as Map<String, Any?> }

        @Suppress("UNCHECKED_CAST")
        private fun stringKeyMap(
            data: Map<String, Any?>,
            key: SerializableKey,
        ): Map<String, Any?>? =
            (data[key.rawValue] as? Map<*, *>)
                ?.takeIf { map -> map.keys.all { it is String } }
                ?.let { it as Map<String, Any?> }

        @Suppress("UNCHECKED_CAST")
        private fun stringList(
            data: Map<String, Any?>,
            key: SerializableKey,
        ): List<String>? =
            (data[key.rawValue] as? List<*>)
                ?.takeIf { list -> list.all { it is String } }
                ?.map { it as String }
    }
}
