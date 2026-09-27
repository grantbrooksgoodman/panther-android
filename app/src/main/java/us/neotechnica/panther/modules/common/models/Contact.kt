//
//  Contact.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 26/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import java.util.Base64

/**
 * A contact from the user's address book.
 *
 * [Contact] captures the subset of a system contact the app uses –
 * identifier, name, and phone numbers.
 *
 * **Note:** the iOS original also carries thumbnail image data and a
 * decoded image; Android reads names and numbers only, so [imageData]
 * is present for structural parity but is not populated by the contacts
 * reader (see [[parity-ii-plan]]).
 */
class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phoneNumbers: List<PhoneNumber>,
    val imageData: ByteArray? = null,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        FIRST_NAME("firstName"),
        ID("id"),
        IMAGE_DATA("imageData"),
        LAST_NAME("lastName"),
        PHONE_NUMBERS("phoneNumbers"),
    }

    // MARK: - Computed Properties

    /**
     * The contact's full name, composed from the non-blank components
     * of their first and last names.
     */
    val fullName: String
        get() =
            when {
                firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
                firstName.isNotBlank() -> firstName
                lastName.isNotBlank() -> lastName
                else -> ""
            }

    /** The uppercased first letters of each word of the contact's full name. */
    val initials: String
        get() = fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.FIRST_NAME.rawValue to firstName,
                Keys.ID.rawValue to id,
                Keys.IMAGE_DATA.rawValue to imageData?.let { Base64.getEncoder().encodeToString(it) },
                Keys.LAST_NAME.rawValue to lastName,
                Keys.PHONE_NUMBERS.rawValue to phoneNumbers.map { it.encoded },
            )

    override val hashFactors: List<String>
        get() =
            listOf(
                firstName,
                id,
                lastName,
                phoneNumbers.joinToString("") { it.encodedHash },
                imageData?.let { Base64.getEncoder().encodeToString(it) } ?: "",
            ).sorted()

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean = other is Contact && hashFactors == other.hashFactors

    override fun hashCode(): Int = hashFactors.hashCode()

    // MARK: - Companion

    companion object {
        /**
         * Reconstructs a contact from its stored encoded map, or `null`
         * if the map is malformed.
         */
        fun decode(data: Map<String, Any?>): Contact? {
            val id = data[Keys.ID.rawValue] as? String ?: return null
            val firstName = data[Keys.FIRST_NAME.rawValue] as? String ?: return null
            val lastName = data[Keys.LAST_NAME.rawValue] as? String ?: return null
            val phoneNumbers =
                (data[Keys.PHONE_NUMBERS.rawValue] as? List<*>)
                    .orEmpty()
                    .mapNotNull { PhoneNumber.decodeSynchronously(it) }
            val imageData = (data[Keys.IMAGE_DATA.rawValue] as? String)?.let { Base64.getDecoder().decode(it) }
            return Contact(
                id = id,
                firstName = firstName,
                lastName = lastName,
                phoneNumbers = phoneNumbers,
                imageData = imageData,
            )
        }
    }
}
