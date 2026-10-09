//
//  PhoneNumberService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import com.google.i18n.phonenumbers.PhoneNumberUtil
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Derives calling codes, hashes, and formatting details from phone
 * number strings.
 *
 * Derivations rely on the app's bundled phone number reference
 * data; results are cached in memory.
 */
object PhoneNumberService {
    // MARK: - Properties

    private val cachedPossibleCallingCodesForNumbers = LockIsolated<Map<String, List<String>>?>(null)
    private val cachedPossibleHashesForNumbers = LockIsolated<Map<String, List<String>>?>(null)
    private val phoneNumberUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    // MARK: - Computed Properties

    /**
     * The calling code for the device's current region, or `1` if it
     * cannot be determined.
     */
    val deviceCallingCode: String
        get() = callingCodes[RegionDetailService.deviceRegionCode] ?: DEFAULT_CALLING_CODE

    private val callingCodes: Map<String, String>
        get() = CommonPropertyLists.callingCodes

    private val lookupTables: Map<String, List<String>>
        get() = CommonPropertyLists.lookupTables

    // MARK: - Calling Code Determination

    /**
     * Returns the calling codes that could plausibly apply to the
     * given number string.
     *
     * A calling code matches if the number begins with it and the
     * remaining digits form a valid length for that code; otherwise,
     * candidates are derived from the number's length alone. Results
     * are cached in memory per number.
     *
     * @param number The phone number string to evaluate, containing
     *   digits only.
     *
     * @return The candidate calling codes; otherwise, `null` if none
     *   could be derived.
     */
    fun possibleCallingCodes(number: String): List<String>? {
        cachedPossibleCallingCodesForNumbers.wrappedValue?.get(number)?.let { return it }

        val countryCodes = matchingCountryCodes(number) ?: callingCodes(number.length)
        if (countryCodes.isNullOrEmpty()) return null

        cachedPossibleCallingCodesForNumbers.withValue {
            it.value = (it.value ?: emptyMap()) + (number to countryCodes)
        }

        return countryCodes
    }

    /**
     * Returns the combined candidate calling codes for the given
     * number strings.
     *
     * Numbers for which no candidates could be derived are skipped.
     *
     * @param numbers The phone number strings to evaluate, containing
     *   digits only.
     *
     * @return The concatenated candidate calling codes for each
     *   number; otherwise, `null` if none could be derived.
     */
    fun possibleCallingCodes(numbers: List<String>): List<String>? =
        numbers
            .flatMap { possibleCallingCodes(it) ?: emptyList() }
            .ifEmpty { null }

    /**
     * Returns a Boolean value indicating whether a national number of
     * the given length is valid for the calling code.
     *
     * @param length The length of the national number.
     * @param callingCode The calling code to validate against.
     *
     * @return `true` if the reference data lists the calling code for
     *   numbers of the given length; otherwise, `false`.
     */
    fun numberIsValidLength(
        length: Int,
        callingCode: String,
    ): Boolean = lookupTables[length.toString()]?.contains(callingCode) == true

    // MARK: - Example National Number String

    /**
     * Returns an example national phone number for the given region,
     * formatted for display.
     *
     * If no example exists for the region, a United States example
     * is returned.
     *
     * @param regionCode The region code for which to produce an
     *   example.
     *
     * @return The formatted example number.
     */
    fun exampleNationalNumberString(regionCode: String): String {
        if (regionCode == US_REGION_CODE) return US_EXAMPLE_NUMBER
        val example =
            phoneNumberUtil.getExampleNumberForType(
                regionCode.uppercase(),
                PhoneNumberUtil.PhoneNumberType.MOBILE,
            ) ?: return US_EXAMPLE_NUMBER

        return phoneNumberUtil.format(example, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
    }

    // MARK: - Hash Generation

    /**
     * Returns the encoded hashes under which the given number string
     * may be stored.
     *
     * The result contains the hash of the full number, plus the hash
     * of the number with each plausible calling code prefix removed.
     * Results are cached in memory per number.
     *
     * @param number The phone number string to evaluate, containing
     *   digits only.
     *
     * @return The candidate hashes.
     */
    fun possibleHashes(number: String): List<String>? {
        cachedPossibleHashesForNumbers.wrappedValue?.get(number)?.let { return it }

        val hashes = mutableListOf(encodedHashOf(listOf(number)))
        matchingCountryCodes(number)?.forEach { code ->
            hashes.add(encodedHashOf(listOf(number.drop(code.length))))
        }

        cachedPossibleHashesForNumbers.withValue {
            it.value = (it.value ?: emptyMap()) + (number to hashes)
        }

        return hashes
    }

    /**
     * Returns the combined candidate hashes for the given number
     * strings.
     *
     * Numbers for which no candidates could be derived are skipped.
     *
     * @param numbers The phone number strings to evaluate, containing
     *   digits only.
     *
     * @return The concatenated candidate hashes for each number;
     *   otherwise, `null` if none could be derived.
     */
    fun possibleHashes(numbers: List<String>): List<String>? =
        numbers
            .flatMap { possibleHashes(it) ?: emptyList() }
            .ifEmpty { null }

    // MARK: - Auxiliary

    private fun callingCodes(numberLength: Int): List<String>? {
        if (lookupTables.isEmpty()) return null
        return lookupTables[numberLength.toString()]
    }

    private fun matchingCountryCodes(number: String): List<String>? {
        if (callingCodes.isEmpty() || lookupTables.isEmpty()) return null

        val matches =
            callingCodes.values.distinct().filter { code ->
                val rawNumberLengthString = number.drop(code.length).length.toString()
                number.startsWith(code) && lookupTables[rawNumberLengthString]?.contains(code) == true
            }

        return if (matches.isEmpty()) null else matches.sorted()
    }
}

private const val DEFAULT_CALLING_CODE = "1"
private const val US_EXAMPLE_NUMBER = "(555) 555-5555"
private const val US_REGION_CODE = "US"
