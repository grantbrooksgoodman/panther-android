//
//  ContactNameService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.services

import us.neotechnica.panther.modules.common.contacts.models.DeviceContact
import us.neotechnica.panther.modules.common.extensions.formattedString

/** Derives a display name from a contact read from the device's address book. */
internal object ContactNameService {
    // MARK: - Name for Contact

    /**
     * Returns the first and last name for the given contact.
     *
     * Resolution proceeds in the following order, using the first
     * source that yields a value:
     *
     * 1. The contact's given and family names, falling back to their
     *    phonetic variants.
     * 2. Whichever of the given or family name is present on its own.
     * 3. The contact's nickname.
     * 4. The contact's organization name, falling back to its phonetic
     *    variant.
     * 5. The contact's first phone number, formatted for display.
     *
     * A value resolved from step 2 or 3 that consists of exactly two
     * words is split into first and last names. Any other single value
     * becomes the last name, with an empty first name.
     *
     * @param contact The contact whose name to resolve.
     *
     * @return A pair of the resolved first and last names; both
     *   components are empty if no source yields a value.
     */
    fun name(contact: DeviceContact): Pair<String, String> {
        val lastName = lastName(contact)
        val firstName = firstName(contact)

        return when {
            lastName != null && firstName != null -> firstName to lastName
            firstName != null -> splitName(firstName) ?: ("" to firstName)
            lastName != null -> splitName(lastName) ?: ("" to lastName)
            else -> nameFromRemainingSources(contact)
        }
    }

    // MARK: - Auxiliary

    private fun firstName(contact: DeviceContact): String? =
        when {
            contact.givenName.isNotBlank() -> contact.givenName
            contact.phoneticGivenName.isNotBlank() -> contact.phoneticGivenName
            else -> null
        }?.trim()

    private fun lastName(contact: DeviceContact): String? =
        when {
            contact.familyName.isNotBlank() -> contact.familyName
            contact.phoneticFamilyName.isNotBlank() -> contact.phoneticFamilyName
            else -> null
        }?.trim()

    private fun nameFromRemainingSources(contact: DeviceContact): Pair<String, String> {
        val nickname = contact.nickname.trim().ifBlank { null }
        if (nickname != null) return splitName(nickname) ?: ("" to nickname)

        val organizationName = organizationName(contact)
        if (organizationName != null) return "" to organizationName

        val phoneNumber = contact.phoneNumbers.firstOrNull()?.formattedString()
        if (phoneNumber != null) return "" to phoneNumber

        return "" to ""
    }

    private fun organizationName(contact: DeviceContact): String? =
        when {
            contact.organizationName.isNotBlank() -> contact.organizationName
            contact.phoneticOrganizationName.isNotBlank() -> contact.phoneticOrganizationName
            else -> null
        }?.trim()

    private fun splitName(string: String): Pair<String, String>? {
        val components = string.trim().split(" ")
        if (components.size != 2) return null
        return components[0].trim() to components[1].trim()
    }
}
