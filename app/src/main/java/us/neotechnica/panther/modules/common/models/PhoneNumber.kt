//
//  PhoneNumber.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.models

import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.networking.modules.common.extensions.decodingFailure
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable

/**
 * A phone number decomposed into its calling code, national
 * number, and region.
 *
 * [PhoneNumber] is the app's canonical phone number
 * representation. Create phone numbers from raw components; the
 * calling code and national number are reduced to their digits.
 *
 * **Important:** Equality and hashing derive from [hashFactors],
 * which include the number's [label] and [internalFormattedString]
 * in addition to its digits. A phone number decoded from the wire
 * carries `null` for both, so it can hash differently from the
 * local instance that produced it.
 */
class PhoneNumber(
    callingCode: String,
    nationalNumberString: String,
    /** The code of the region the number belongs to. */
    val regionCode: String,
    /** The label associated with the number, or `null`. */
    val label: String?,
    /**
     * The system-provided international format of the number, or
     * `null`.
     */
    val internalFormattedString: String?,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Type Aliases

    private enum class Keys(
        val rawValue: String,
    ) {
        CALLING_CODE("callingCode"),
        NATIONAL_NUMBER_STRING("nationalNumberString"),
        REGION_CODE("regionCode"),
    }

    // MARK: - Properties

    /** The number's calling code, containing digits only. */
    val callingCode: String = callingCode.digits

    /** The number's national portion, containing digits only. */
    val nationalNumberString: String = nationalNumberString.digits

    // MARK: - Computed Properties

    /**
     * The number's compiled number string: its calling code followed
     * by its national number, containing digits only.
     */
    val compiledNumberString: String get() = callingCode + nationalNumberString

    /**
     * The serialized representation of the phone number.
     *
     * The [label] and [internalFormattedString] are not
     * serialized.
     */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.CALLING_CODE.rawValue to callingCode,
                Keys.NATIONAL_NUMBER_STRING.rawValue to nationalNumberString,
                Keys.REGION_CODE.rawValue to regionCode,
            )

    override val hashFactors: List<String>
        get() =
            listOf(
                callingCode,
                internalFormattedString ?: "",
                label ?: "",
                nationalNumberString,
                regionCode,
            ).sorted()

    // MARK: - Equatable Conformance

    override fun equals(other: Any?): Boolean = other is PhoneNumber && hashFactors == other.hashFactors

    override fun hashCode(): Int = hashFactors.hashCode()

    // MARK: - Companion

    companion object : SerializableDecoder<PhoneNumber, Map<String, Any?>> {
        override fun canDecode(data: Map<String, Any?>): Boolean {
            val callingCode = data[Keys.CALLING_CODE.rawValue] as? String
            val nationalNumberString = data[Keys.NATIONAL_NUMBER_STRING.rawValue] as? String
            return callingCode?.digits?.isNotBlank() == true &&
                nationalNumberString?.digits?.isNotBlank() == true &&
                data[Keys.REGION_CODE.rawValue] is String
        }

        override fun decode(data: Map<String, Any?>): PhoneNumber {
            val callingCode = data[Keys.CALLING_CODE.rawValue] as? String
            val nationalNumberString = data[Keys.NATIONAL_NUMBER_STRING.rawValue] as? String
            val regionCode = data[Keys.REGION_CODE.rawValue] as? String

            if (callingCode == null || nationalNumberString == null || regionCode == null) {
                throw decodingFailure(this, data)
            }

            return PhoneNumber(
                callingCode = callingCode,
                nationalNumberString = nationalNumberString,
                regionCode = regionCode,
                label = null,
                internalFormattedString = null,
            )
        }

        /**
         * Reconstructs a phone number from its stored encoded map
         * without suspending, or `null` if the map is malformed. Used
         * by the file-backed archives, whose decode step is synchronous.
         *
         * @param element The stored encoded map.
         */
        fun decodeSynchronously(element: Any?): PhoneNumber? {
            val data = element as? Map<*, *> ?: return null
            val callingCode = data[Keys.CALLING_CODE.rawValue] as? String ?: return null
            val nationalNumberString = data[Keys.NATIONAL_NUMBER_STRING.rawValue] as? String ?: return null
            val regionCode = data[Keys.REGION_CODE.rawValue] as? String ?: return null
            return PhoneNumber(
                callingCode = callingCode,
                nationalNumberString = nationalNumberString,
                regionCode = regionCode,
                label = null,
                internalFormattedString = null,
            )
        }

        /**
         * Creates a phone number from the given string, deriving its
         * components.
         *
         * The calling code is derived by testing the number's possible
         * calling codes against the device's, then validating the
         * remaining digits' length. The region is derived similarly,
         * preferring the device's region. Components that cannot be
         * derived fall back to the device's calling code and region.
         *
         * @param string The phone number string. Non-digit characters
         *   are ignored.
         * @param label The label to associate with the number, if any.
         *
         * @return A phone number with the derived components.
         */
        operator fun invoke(
            string: String,
            label: String? = null,
        ): PhoneNumber {
            var numberValue = string.digits
            val deviceCallingCode = PhoneNumberService.deviceCallingCode
            val deviceRegionCode = RegionDetailService.deviceRegionCode

            var callingCode: String? = null
            val possibleCallingCodes = PhoneNumberService.possibleCallingCodes(numberValue)
            if (possibleCallingCodes != null &&
                (possibleCallingCodes.contains(deviceCallingCode) || possibleCallingCodes.size == 1)
            ) {
                val derivedCallingCode =
                    possibleCallingCodes.firstOrNull { it == deviceCallingCode } ?: possibleCallingCodes.firstOrNull()
                if (!derivedCallingCode.isNullOrBlank()) callingCode = derivedCallingCode
            }

            var resolvedCallingCode = callingCode ?: deviceCallingCode
            if (numberValue.startsWith(resolvedCallingCode)) {
                numberValue = numberValue.drop(resolvedCallingCode.length)
            }

            if (!PhoneNumberService.numberIsValidLength(numberValue.length, resolvedCallingCode)) {
                resolvedCallingCode = PhoneNumberService.possibleCallingCodes(numberValue)?.firstOrNull() ?: resolvedCallingCode
                if (resolvedCallingCode.isBlank()) resolvedCallingCode = deviceCallingCode
            }

            val possibleRegionCodes = RegionDetailService.regionCodes(resolvedCallingCode)
            val regionCode =
                when {
                    possibleRegionCodes.contains(deviceRegionCode) -> deviceRegionCode
                    resolvedCallingCode == "1" -> "US"
                    else -> possibleRegionCodes.singleOrNull()?.takeIf { it.isNotBlank() }
                }

            return PhoneNumber(
                callingCode = resolvedCallingCode,
                nationalNumberString = numberValue,
                regionCode = regionCode ?: deviceRegionCode,
                label = label?.trim(),
                internalFormattedString = null,
            )
        }
    }
}
