//
//  ContactPair.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import us.neotechnica.panther.networking.modules.common.interfaces.Serializable

/**
 * A device contact paired with the phone numbers that map it to
 * registered users.
 *
 * A contact pair associates a [Contact] with one or more [NumberPair]
 * values – the contact's phone numbers together with the registered
 * users they resolve to. Use contact pairs to determine which of the
 * user's contacts can be messaged.
 */
class ContactPair(
    val contact: Contact,
    val numberPairs: List<NumberPair>,
) : Serializable<Map<String, Any?>> {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        CONTACT("contact"),
        NUMBER_PAIRS("numberPairs"),
    }

    // MARK: - Computed Properties

    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.CONTACT.rawValue to contact.encoded,
                Keys.NUMBER_PAIRS.rawValue to numberPairs.map { it.encoded },
            )

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean =
        other is ContactPair && contact == other.contact && numberPairs == other.numberPairs

    override fun hashCode(): Int {
        var result = contact.id.hashCode()
        result = 31 * result + numberPairs.map { it.phoneNumber.compiledNumberString }.hashCode()
        result = 31 * result + numberPairs.flatMap { it.userIDs }.hashCode()
        return result
    }

    // MARK: - Companion

    companion object {
        /**
         * Reconstructs a contact pair from its stored encoded map, or
         * `null` if the map is malformed.
         */
        fun decode(data: Map<String, Any?>): ContactPair? {
            val contactMap = data[Keys.CONTACT.rawValue] as? Map<*, *> ?: return null
            @Suppress("UNCHECKED_CAST")
            val contact = Contact.decode(contactMap as Map<String, Any?>) ?: return null
            val numberPairs =
                (data[Keys.NUMBER_PAIRS.rawValue] as? List<*>)
                    .orEmpty()
                    .mapNotNull { element ->
                        (element as? Map<*, *>)?.let {
                            @Suppress("UNCHECKED_CAST")
                            NumberPair.decode(it as Map<String, Any?>)
                        }
                    }
            if (numberPairs.isEmpty()) return null
            return ContactPair(contact = contact, numberPairs = numberPairs)
        }
    }
}
