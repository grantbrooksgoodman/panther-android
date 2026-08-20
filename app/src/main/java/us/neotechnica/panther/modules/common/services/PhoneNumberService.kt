//
//  PhoneNumberService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import com.google.i18n.phonenumbers.PhoneNumberUtil
import us.neotechnica.panther.networking.modules.common.extensions.digits

/**
 * Answers questions about phone numbers – calling codes, valid
 * lengths, and example numbers – from [CommonPropertyLists] and
 * libphonenumber. Ported from the iOS `PhoneNumberService`.
 */
object PhoneNumberService {
    // MARK: - Properties

    private val phoneNumberUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    // MARK: - Computed Properties

    /** The device's calling code, e.g. `"1"`; defaults to `"1"`. */
    val deviceCallingCode: String
        get() = RegionDetailService.callingCode(RegionDetailService.deviceRegionCode) ?: DEFAULT_CALLING_CODE

    // MARK: - Methods

    /**
     * Returns the calling codes a number could belong to, matching by
     * prefix and length, or `null` if none apply.
     */
    fun possibleCallingCodes(number: String): List<String>? {
        val matches = matchingCallingCodes(number)
        if (matches != null) return matches
        return callingCodes(number.digits.length)
    }

    /**
     * Returns a Boolean value indicating whether a national number of
     * the given length is valid for the calling code.
     */
    fun numberIsValidLength(
        length: Int,
        callingCode: String,
    ): Boolean = CommonPropertyLists.lookupTables[length.toString()]?.contains(callingCode) == true

    /** An example national number for the region, for placeholder text. */
    fun exampleNationalNumberString(regionCode: String): String {
        val example =
            phoneNumberUtil.getExampleNumberForType(
                regionCode.uppercase(),
                PhoneNumberUtil.PhoneNumberType.MOBILE,
            ) ?: return US_EXAMPLE_NUMBER
        return phoneNumberUtil.format(example, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
    }

    // MARK: - Auxiliary

    private fun callingCodes(numberLength: Int): List<String>? = CommonPropertyLists.lookupTables[numberLength.toString()]

    private fun matchingCallingCodes(number: String): List<String>? {
        val digits = number.digits
        val callingCodes = CommonPropertyLists.callingCodes.values.distinct()
        val matches = mutableListOf<String>()
        for (code in callingCodes) {
            if (!digits.startsWith(code)) continue
            val remainingLength = digits.drop(code.length).length
            if (CommonPropertyLists.lookupTables[remainingLength.toString()]?.contains(code) == true) {
                matches.add(code)
            }
        }
        return if (matches.isEmpty()) null else matches.sorted()
    }

    // MARK: - Companion

    private const val DEFAULT_CALLING_CODE = "1"
    private const val US_EXAMPLE_NUMBER = "(555) 555-5555"
}
