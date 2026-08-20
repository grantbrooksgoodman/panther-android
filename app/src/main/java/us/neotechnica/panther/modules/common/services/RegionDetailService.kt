//
//  RegionDetailService.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import java.util.Locale

/**
 * Resolves region metadata – calling codes, localized names, and
 * emoji flags – from [CommonPropertyLists], ported from the iOS
 * `RegionDetailService`.
 *
 * **Note:** the iOS service renders flag images; this Android port
 * uses emoji flags derived from the region code.
 */
object RegionDetailService {
    // MARK: - Computed Properties

    /** The device's current region code, e.g. `"US"`. */
    val deviceRegionCode: String
        get() =
            Locale
                .getDefault()
                .country
                .ifBlank { FALLBACK_REGION_CODE }
                .uppercase()

    /** Every known region code, sorted by localized display name. */
    val allRegionCodes: List<String>
        get() = CommonPropertyLists.callingCodes.keys.sortedBy { localizedRegionName(it) }

    // MARK: - Methods

    /** The calling code for the given region code, or `null`. */
    fun callingCode(regionCode: String): String? = CommonPropertyLists.callingCodes[regionCode.uppercase()]

    /** Every region code that shares the given calling code. */
    fun regionCodes(callingCode: String): List<String> =
        CommonPropertyLists.callingCodes
            .filterValues { it == callingCode }
            .keys
            .sorted()

    /** The localized display name of the region, e.g. `"United States"`. */
    fun localizedRegionName(regionCode: String): String {
        val name =
            Locale
                .Builder()
                .setRegion(regionCode.uppercase())
                .build()
                .displayCountry
        return name.ifBlank { regionCode.uppercase() }
    }

    /** The emoji flag for the region, e.g. `"🇺🇸"`. */
    fun emojiFlag(regionCode: String): String {
        val code = regionCode.uppercase()
        if (code.length != REGION_CODE_LENGTH || code.any { it !in 'A'..'Z' }) return ""
        return buildString {
            for (character in code) appendCodePoint(REGIONAL_INDICATOR_BASE + (character - 'A'))
        }
    }

    /**
     * A display title for the region, e.g. `"🇺🇸  United States  (+1)"`.
     */
    fun regionTitle(regionCode: String): String {
        val flag = emojiFlag(regionCode)
        val name = localizedRegionName(regionCode)
        val callingCode = callingCode(regionCode)?.let { " (+$it)" } ?: ""
        return "$flag  $name$callingCode".trim()
    }

    // MARK: - Companion

    private const val FALLBACK_REGION_CODE = "US"
    private const val REGION_CODE_LENGTH = 2
    private const val REGIONAL_INDICATOR_BASE = 0x1F1E6
}
