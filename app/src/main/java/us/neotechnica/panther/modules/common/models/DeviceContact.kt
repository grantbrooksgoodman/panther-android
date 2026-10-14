//
//  DeviceContact.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PhoneNumberService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency

/**
 * A contact read from the device's address book, grouped by its
 * address-book identifier.
 *
 * Carries the raw name fields the device exposes across its structured
 * name, nickname, and organization records, the contact's thumbnail
 * image data, and the merged phone numbers belonging to the contact.
 * These fields feed name resolution and the matching of contacts to
 * registered users.
 */
internal class DeviceContact(
    val id: String,
    val givenName: String,
    val familyName: String,
    val phoneticGivenName: String,
    val phoneticFamilyName: String,
    val nickname: String,
    val organizationName: String,
    val phoneticOrganizationName: String,
    val phoneNumbers: List<PhoneNumber>,
    val imageData: ByteArray?,
) {
    // MARK: - Dependencies

    private val phoneNumberService: PhoneNumberService by Dependency { it.commonServices.phoneNumber }

    // MARK: - Methods

    /**
     * Returns a Boolean value that indicates whether any of the
     * contact's phone numbers is the given phone number.
     *
     * A number matches on compiled-number equality – the device number
     * with or without its leading calling code – or on national-number
     * equality once the number's length validates for the given
     * number's calling code.
     *
     * @param phoneNumber The phone number to match.
     */
    fun matches(phoneNumber: PhoneNumber): Boolean {
        val compiledNumberString = phoneNumber.compiledNumberString
        val nationalNumberString = phoneNumber.nationalNumberString
        if (compiledNumberString.isEmpty()) return false

        return phoneNumbers.any { number ->
            number.compiledNumberString == compiledNumberString ||
                (
                    nationalNumberString.isNotEmpty() &&
                        number.nationalNumberString == nationalNumberString &&
                        phoneNumberService.numberIsValidLength(nationalNumberString.length, phoneNumber.callingCode)
                )
        }
    }

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean = other is DeviceContact && other.id == id

    override fun hashCode(): Int = id.hashCode()
}
