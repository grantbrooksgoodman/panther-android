//
//  PhoneNumber.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength")

package us.neotechnica.panther.modules.common.models

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.RegionDetailService
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.common.interfaces.Serializable
import us.neotechnica.panther.networking.modules.common.interfaces.SerializableDecoder
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.EncodedHashable
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * A phone number decomposed into its calling code, national number,
 * and region.
 *
 * [PhoneNumber] is the app's canonical phone number representation.
 * Its [compiledNumberString] identifies a number across the app, such
 * as when matching contacts to registered users.
 *
 * Create phone numbers from raw components or from a string; the
 * string factory derives the calling code and region using the
 * device's region and number length validation.
 *
 * Every stored property is immutable except the per-region formatted
 * string cache, which is lock-isolated; a phone number can therefore
 * be formatted concurrently from any thread.
 *
 * **Important:** Equality and hashing derive from [hashFactors], which
 * include the number's [label] and [internalFormattedString] in
 * addition to its digits. Two phone numbers with the same digits but
 * different labels are not equal.
 */
class PhoneNumber(
    callingCode: String,
    nationalNumberString: String,
    /** The code of the region the number belongs to. */
    val regionCode: String,
    label: String?,
    /**
     * The system-provided international format of the number, or
     * `null`. Used as a formatting fallback.
     */
    val internalFormattedString: String?,
) : Serializable<Map<String, Any?>>,
    EncodedHashable {
    // MARK: - Dependencies

    private val regionDetailService: RegionDetailService by Dependency { it.commonServices.regionDetail }

    // MARK: - Types

    private enum class Keys(
        val rawValue: String,
    ) {
        CALLING_CODE("callingCode"),
        NATIONAL_NUMBER_STRING("nationalNumberString"),
        REGION_CODE("regionCode"),
    }

    /** A formatted string, cached alongside the code of the region it was formatted for. */
    private class FormattedStringCacheEntry(
        val formattedString: String,
        val regionCode: String,
    )

    // MARK: - Properties

    /** The number's calling code, containing digits only. */
    val callingCode: String = callingCode.digits

    /** The label associated with the number, such as Home or Mobile, or `null`. */
    val label: String? = label?.trim()

    /** The number's national portion, containing digits only. */
    val nationalNumberString: String = nationalNumberString.digits

    private val formattedStringCache = LockIsolated<FormattedStringCacheEntry?>(null)

    // MARK: - Computed Properties

    /**
     * The number's compiled number string.
     *
     * A compiled number string is a phone number's calling code
     * followed by its national number, containing digits only.
     */
    val compiledNumberString: String get() = callingCode + nationalNumberString

    /**
     * The serialized representation of the phone number.
     *
     * The [label] and [internalFormattedString] are not serialized.
     */
    override val encoded: Map<String, Any?>
        get() =
            mapOf(
                Keys.CALLING_CODE.rawValue to callingCode,
                Keys.NATIONAL_NUMBER_STRING.rawValue to nationalNumberString,
                Keys.REGION_CODE.rawValue to regionCode,
            )

    /**
     * The persisted representation of the phone number, carrying every
     * stored property – the [label] and [internalFormattedString]
     * included – so an archived number reloads equal to the original.
     */
    val archived: Map<String, Any?>
        get() =
            encoded +
                mapOf(
                    ARCHIVE_KEY_INTERNAL_FORMATTED_STRING to internalFormattedString,
                    ARCHIVE_KEY_LABEL to label,
                )

    /**
     * The strings that collectively define this instance's identity
     * for hashing purposes, sorted alphabetically.
     *
     * Contains the calling code, national number, region code, label,
     * and internal formatted string.
     */
    override val hashFactors: List<String>
        get() =
            listOf(
                callingCode,
                internalFormattedString ?: "",
                label ?: "",
                nationalNumberString,
                regionCode,
            ).sorted()

    // MARK: - Formatted Strings

    /**
     * Returns the number formatted for display, prefixed with its
     * calling code.
     *
     * The formatted result is cached per region; formatting again for
     * the same region returns the cached string. The cache is
     * lock-isolated, so this method is safe to call concurrently.
     *
     * @param regionCode The code of the region whose formatting
     *   conventions to use. Pass `null` to use the number's region.
     *
     * @return The formatted number, prefixed with `+` and the calling
     *   code.
     */
    fun formattedString(regionCode: String? = null): String {
        val resolvedRegionCode = regionCode ?: this.regionCode

        formattedStringCache.wrappedValue
            ?.takeIf { it.regionCode == resolvedRegionCode }
            ?.let { return it.formattedString }

        val partiallyFormatted = partiallyFormatted(resolvedRegionCode)

        if (partiallyFormatted.digits.startsWith(callingCode)) {
            if (partiallyFormatted.startsWith("+")) return partiallyFormatted
            return "+${partiallyFormatted.trimStart()}"
        }

        val formattedString = "+$callingCode ${partiallyFormatted.trimStart()}"
        formattedStringCache.wrappedValue = FormattedStringCacheEntry(formattedString, resolvedRegionCode)

        return formattedString
    }

    /**
     * Returns the national number formatted for the given region's
     * conventions, without a calling code prefix.
     *
     * @param regionCode The code of the region whose formatting
     *   conventions to use. Pass `null` to use the number's region.
     *
     * @return The formatted national number.
     */
    fun partiallyFormatted(regionCode: String? = null): String {
        val resolvedRegionCode = regionCode ?: this.regionCode

        val partialNumberString = nationalNumberString.ifEmpty { compiledNumberString }
        if (partialNumberString.isEmpty()) return partialNumberString

        var fullFormatAttempt = formattedString(regionCode = resolvedRegionCode, includeCallingCode = false)

        val regionCallingCode = regionDetailService.callingCode(resolvedRegionCode) ?: return fullFormatAttempt

        if (fullFormatAttempt != failsafeFormat(partialNumberString)) {
            if (!fullFormatAttempt.startsWith("+$regionCallingCode")) {
                return formatPartial(partialNumberString, resolvedRegionCode)
            }

            fullFormatAttempt = fullFormatAttempt.replace("+$regionCallingCode", "")
            return fullFormatAttempt.trim()
        }

        return formatPartial(partialNumberString, resolvedRegionCode)
    }

    private fun failsafeFormat(numberString: String): String {
        val digits = numberString.digits
        val evenDigits = digits.length % 2 == 0

        val formattedString = StringBuilder()
        for ((index, character) in digits.withIndex()) {
            when {
                index == 0 -> formattedString.append(character)
                index % 2 != 0 -> formattedString.append(if (evenDigits) "" else " ").append(character)
                else -> formattedString.append(if (evenDigits) " " else "").append(character)
            }
        }

        return formattedString.toString().trim()
    }

    private fun formatPartial(
        numberString: String,
        regionCode: String,
    ): String {
        val formatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(regionCode.uppercase())
        var formatted = ""
        for (character in numberString.digits) formatted = formatter.inputDigit(character)
        return formatted.trim()
    }

    private fun formattedString(
        regionCode: String,
        includeCallingCode: Boolean,
    ): String {
        if (callingCode == "1" && internalFormattedString?.contains("+") == true) {
            return internalFormattedString
        }

        val fallbackFormatted = failsafeFormat(compiledNumberString)

        return try {
            val utility = PhoneNumberUtil.getInstance()
            val parsed = utility.parse(nationalNumberString, regionCode.uppercase())
            val format =
                if (includeCallingCode) {
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                } else {
                    PhoneNumberUtil.PhoneNumberFormat.NATIONAL
                }
            utility.format(parsed, format) ?: internalFormattedString ?: fallbackFormatted
        } catch (_: NumberParseException) {
            internalFormattedString ?: fallbackFormatted
        }
    }

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
                throw Exception.Networking.decodingFailed(
                    data,
                    ExceptionMetadata(this),
                )
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
                label = data[ARCHIVE_KEY_LABEL] as? String,
                internalFormattedString = data[ARCHIVE_KEY_INTERNAL_FORMATTED_STRING] as? String,
            )
        }

        private const val ARCHIVE_KEY_INTERNAL_FORMATTED_STRING = "internalFormattedString"
        private const val ARCHIVE_KEY_LABEL = "label"

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
            internalFormattedString: String? = null,
        ): PhoneNumber {
            var numberValue = string.digits
            val deviceCallingCode = DependencyValues.current.commonServices.phoneNumber.deviceCallingCode
            val deviceRegionCode = DependencyValues.current.commonServices.regionDetail.deviceRegionCode

            var callingCode: String? = null
            val possibleCallingCodes =
                DependencyValues.current.commonServices.phoneNumber
                    .possibleCallingCodes(numberValue)
            if (possibleCallingCodes != null &&
                (possibleCallingCodes.contains(deviceCallingCode) || possibleCallingCodes.size == 1)
            ) {
                val derivedCallingCode =
                    possibleCallingCodes.firstOrNull { it == deviceCallingCode } ?: possibleCallingCodes.firstOrNull()
                if (!derivedCallingCode.isNullOrBlank()) callingCode = derivedCallingCode
            } else if (internalFormattedString != null) {
                // The international form carries the calling code up
                // front; in its E.164 form it is the code the digits
                // begin with.
                callingCode = callingCodeFromInternationalString(internalFormattedString)
            }

            var resolvedCallingCode = callingCode ?: deviceCallingCode
            if (numberValue.startsWith(resolvedCallingCode)) {
                numberValue = numberValue.drop(resolvedCallingCode.length)
            }

            if (!DependencyValues.current.commonServices.phoneNumber
                    .numberIsValidLength(numberValue.length, resolvedCallingCode)
            ) {
                resolvedCallingCode =
                    DependencyValues.current.commonServices.phoneNumber
                        .possibleCallingCodes(numberValue)
                        ?.firstOrNull()
                        ?: resolvedCallingCode
                if (resolvedCallingCode.isBlank()) resolvedCallingCode = deviceCallingCode
            }

            val possibleRegionCodes =
                DependencyValues.current.commonServices.regionDetail.regionCodes(
                    RegionDetailService.QueryStrategy.CallingCode(resolvedCallingCode),
                )
                    ?: emptyList()
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
                label = label,
                internalFormattedString = internalFormattedString,
            )
        }

        private fun callingCodeFromInternationalString(internationalString: String): String? {
            val firstComponent =
                internationalString
                    .split(" ")
                    .firstOrNull()
                    ?.digits
                    .orEmpty()
            if (firstComponent.isNotBlank() && firstComponent.length <= MAXIMUM_CALLING_CODE_LENGTH) return firstComponent

            val digits = internationalString.digits
            return DependencyValues.current.commonServices.phoneNumber
                .possibleCallingCodes(digits)
                ?.firstOrNull { digits.startsWith(it) }
                ?.takeIf { it.isNotBlank() }
        }

        private const val MAXIMUM_CALLING_CODE_LENGTH = 3
    }
}
