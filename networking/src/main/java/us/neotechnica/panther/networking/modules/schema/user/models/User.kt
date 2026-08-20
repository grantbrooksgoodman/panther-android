//
//  User.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.schema.user.models

import us.neotechnica.panther.networking.modules.common.extensions.bangQualifiedEmptyList
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash

/**
 * A registered user.
 *
 * A user carries their identity, phone number, language, and
 * preferences, along with the identifiers of their conversations,
 * blocked users, and push tokens.
 *
 * **Note:** `badgeNumber` is not part of the serialized user; it
 * is seeded at creation and written out-of-band at
 * `users/<id>/badgeNumber`.
 */
data class User(
    /** The user's unique identifier. */
    val id: String,
    /** A Boolean value that indicates whether AI-enhanced translations are enabled. */
    val aiEnhancedTranslationsEnabled: Boolean,
    /** The identifiers of the users this user has blocked, or `null`. */
    val blockedUserIDs: List<String>?,
    /** The identifiers of the user's conversations, or `null`. */
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
    /** The user's previously-used language codes, or `null`. */
    val previousLanguageCodes: List<String>?,
    /** The user's push notification tokens, or `null`. */
    val pushTokens: List<String>?,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        ID("id"),
        AI_ENHANCED_TRANSLATIONS_ENABLED("aiEnhancedTranslationsEnabled"),
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

    /** The serialized representation of the user. */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.ID.rawValue to id,
                Keys.AI_ENHANCED_TRANSLATIONS_ENABLED.rawValue to aiEnhancedTranslationsEnabled,
                Keys.BLOCKED_USER_IDS.rawValue to
                    (blockedUserIDs?.takeIf { it.isNotEmpty() }?.associateWith { true } ?: emptyMap()),
                Keys.CONVERSATION_IDS.rawValue to
                    (
                        conversationIDs
                            ?.takeIf { it.isNotEmpty() }
                            ?.associate { it.key to it.hash } ?: emptyMap<String, String>()
                    ),
                Keys.DEVICE_ID.rawValue to deviceID,
                Keys.IS_PEN_PALS_PARTICIPANT.rawValue to isPenPalsParticipant,
                Keys.LANGUAGE_CODE.rawValue to languageCode,
                Keys.MESSAGE_RECIPIENT_CONSENT_REQUIRED.rawValue to messageRecipientConsentRequired,
                Keys.PHONE_NUMBER.rawValue to phoneNumber.encoded,
                Keys.PREVIOUS_LANGUAGE_CODES.rawValue to (previousLanguageCodes ?: bangQualifiedEmptyList),
                Keys.PUSH_TOKENS.rawValue to
                    (pushTokens?.takeIf { it.isNotEmpty() }?.associateWith { true } ?: emptyMap()),
            )

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

    // MARK: - Companion

    companion object : SerializableDecoder<User, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean {
            if (data[Keys.ID.rawValue] !is String) return false
            if (data[Keys.AI_ENHANCED_TRANSLATIONS_ENABLED.rawValue] !is Boolean) return false
            if (data[Keys.DEVICE_ID.rawValue] !is String) return false
            if (data[Keys.IS_PEN_PALS_PARTICIPANT.rawValue] !is Boolean) return false
            if (data[Keys.LANGUAGE_CODE.rawValue] !is String) return false
            if (data[Keys.MESSAGE_RECIPIENT_CONSENT_REQUIRED.rawValue] !is Boolean) return false
            if (data[Keys.PREVIOUS_LANGUAGE_CODES.rawValue] !is List<*>) return false

            val phoneNumber = mapValue(data, Keys.PHONE_NUMBER) ?: return false
            return PhoneNumber.canDecode(phoneNumber)
        }

        override suspend fun decode(data: Map<String, Any?>): User {
            val id = data[Keys.ID.rawValue] as? String
            val aiEnhanced = data[Keys.AI_ENHANCED_TRANSLATIONS_ENABLED.rawValue] as? Boolean
            val deviceID = data[Keys.DEVICE_ID.rawValue] as? String
            val encodedPhoneNumber = mapValue(data, Keys.PHONE_NUMBER)
            val isPenPalsParticipant = data[Keys.IS_PEN_PALS_PARTICIPANT.rawValue] as? Boolean
            val languageCode = data[Keys.LANGUAGE_CODE.rawValue] as? String
            val consentRequired = data[Keys.MESSAGE_RECIPIENT_CONSENT_REQUIRED.rawValue] as? Boolean
            val previousLanguageCodes = stringList(data, Keys.PREVIOUS_LANGUAGE_CODES)

            if (id == null ||
                aiEnhanced == null ||
                deviceID == null ||
                encodedPhoneNumber == null ||
                isPenPalsParticipant == null ||
                languageCode == null ||
                consentRequired == null ||
                previousLanguageCodes == null
            ) {
                throw decodingFailure(this, data)
            }

            // Dictionaries carry no order; sort map-derived arrays so
            // re-decodes of identical data compare equal.
            val blockedUserIDs = stringKeyMap(data, Keys.BLOCKED_USER_IDS)?.keys?.sorted() ?: emptyList()
            val conversationIDs =
                stringKeyMap(data, Keys.CONVERSATION_IDS)
                    ?.map { (key, value) -> ConversationID(key = key, hash = value.toString()) }
                    ?.sortedBy { it.key }
                    ?: emptyList()
            val pushTokens = stringKeyMap(data, Keys.PUSH_TOKENS)?.keys?.sorted() ?: emptyList()

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

        @Suppress("UNCHECKED_CAST")
        private fun mapValue(
            data: Map<String, Any?>,
            key: Keys,
        ): Map<String, Any?>? = (data[key.rawValue] as? Map<*, *>)?.let { it as Map<String, Any?> }

        @Suppress("UNCHECKED_CAST")
        private fun stringKeyMap(
            data: Map<String, Any?>,
            key: Keys,
        ): Map<String, Any?>? =
            (data[key.rawValue] as? Map<*, *>)
                ?.takeIf { map -> map.keys.all { it is String } }
                ?.let { it as Map<String, Any?> }

        @Suppress("UNCHECKED_CAST")
        private fun stringList(
            data: Map<String, Any?>,
            key: Keys,
        ): List<String>? =
            (data[key.rawValue] as? List<*>)
                ?.takeIf { list -> list.all { it is String } }
                ?.map { it as String }
    }
}
