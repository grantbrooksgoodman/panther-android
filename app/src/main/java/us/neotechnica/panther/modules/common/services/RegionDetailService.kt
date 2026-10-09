//
//  RegionDetailService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import us.neotechnica.panther.modules.common.extensions.localizedString
import us.neotechnica.panther.modules.common.models.FlagImage
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import java.util.Locale

/**
 * Looks up region details – names, titles, calling codes, and
 * images – for the regions in the app's phone number reference
 * data.
 *
 * Lookup results are cached in memory.
 */
object RegionDetailService {
    // MARK: - Types

    /**
     * A criterion for querying region details.
     *
     * Each query method supports a subset of strategies;
     * unsupported strategies return `null`.
     */
    sealed interface QueryStrategy {
        /** Queries by international calling code. */
        data class CallingCode(
            val callingCode: String,
        ) : QueryStrategy

        /** Queries by region code. */
        data class RegionCode(
            val regionCode: String,
        ) : QueryStrategy

        /** Queries by formatted region title. */
        data class RegionTitle(
            val regionTitle: String,
        ) : QueryStrategy

        /** Queries by free-form search term. */
        data class SearchTerm(
            val searchTerm: String,
        ) : QueryStrategy
    }

    /**
     * The arrangement of a region title's components.
     *
     * A region title combines a region's name with its calling
     * code – for example, `+1 (United States)` or
     * `United States (+1)`.
     */
    enum class RegionTitleFormat {
        /** Places the calling code before the region name. */
        CALLING_CODE_FIRST,

        /** Places the region name before the calling code. */
        REGION_NAME_FIRST,
    }

    // MARK: - Properties

    private val cachedImagesForRegionCodes = LockIsolated<Map<String, FlagImage>?>(null)
    private val cachedImagesForRegionTitles = LockIsolated<Map<String, FlagImage>?>(null)
    private val cachedLocalizedRegionNamesForRegionCodes = LockIsolated<Map<String, String>?>(null)
    private val cachedRegionTitlesForAllCallingCodes = LockIsolated<List<String>?>(null)
    private val cachedRegionTitlesForCallingCodes = LockIsolated<Map<String, Pair<String, RegionTitleFormat>>?>(null)
    private val cachedRegionTitlesForRegionCodes = LockIsolated<Map<String, Pair<String, RegionTitleFormat>>?>(null)

    @Volatile
    private var appContext: Context? = null

    // MARK: - Computed Properties

    /**
     * The region code for the device's current region, or `US` if
     * it cannot be determined.
     */
    val deviceRegionCode: String
        get() = Locale.getDefault().country.ifBlank { FALLBACK_REGION_CODE }

    private val callingCodes: Map<String, String>
        get() = CommonPropertyLists.callingCodes

    private val systemLocalizedLocale: Locale
        get() = Locale(RuntimeStorage.languageCode)

    // MARK: - Init

    /** Prepares the service with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Calling Codes

    /**
     * Returns the international calling code for the given region
     * code.
     *
     * @param regionCode The region code to look up.
     *
     * @return The region's calling code; otherwise, `null` if the
     *   region code is unknown.
     */
    fun callingCode(regionCode: String): String? = callingCodes[regionCode.uppercase()]

    // MARK: - Images

    /**
     * Returns the image for the region matching the given strategy.
     *
     * Supported strategies are [QueryStrategy.RegionCode] and
     * [QueryStrategy.RegionTitle].
     *
     * @param strategy The criterion by which to find the region.
     *
     * @return The region's image; otherwise, `null` if no match
     *   exists or the strategy is unsupported.
     */
    fun image(strategy: QueryStrategy): FlagImage? {
        val context = appContext ?: return null
        val keys = callingCodes.keys

        return when (strategy) {
            is QueryStrategy.RegionCode -> {
                cachedImagesForRegionCodes.wrappedValue?.get(strategy.regionCode)?.let { return it }

                val match = keys.firstOrNull { it == strategy.regionCode } ?: return null
                val image = FlagImage.named(match, context) ?: return null

                cachedImagesForRegionCodes.insert(strategy.regionCode to image)
                image
            }

            is QueryStrategy.RegionTitle -> {
                cachedImagesForRegionTitles.wrappedValue?.get(strategy.regionTitle)?.let { return it }

                val match = regionCodesForRegionTitle(strategy.regionTitle).firstOrNull() ?: return null
                val image = FlagImage.named(match, context) ?: return null

                cachedImagesForRegionTitles.insert(strategy.regionTitle to image)
                image
            }

            is QueryStrategy.CallingCode,
            is QueryStrategy.SearchTerm,
            -> null
        }
    }

    // MARK: - Region Codes

    /**
     * Returns the region code matching the given strategy.
     *
     * Supported strategies are [QueryStrategy.CallingCode] and
     * [QueryStrategy.RegionTitle]. When multiple regions share the
     * given calling code, a localized "Multiple" placeholder is
     * returned.
     *
     * @param strategy The criterion by which to find the region
     *   code.
     *
     * @return The matching region code; otherwise, `null` if no
     *   match exists or the strategy is unsupported.
     */
    fun regionCode(strategy: QueryStrategy): String? =
        when (strategy) {
            is QueryStrategy.CallingCode -> {
                val regionCodes = regionCodesForCallingCode(callingCode = strategy.callingCode)
                when {
                    regionCodes == null -> null
                    regionCodes.size != 1 -> LocalizedStringKey.Multiple.localized()
                    else -> regionCodes[0]
                }
            }

            is QueryStrategy.RegionTitle -> regionCodesForRegionTitle(regionTitle = strategy.regionTitle)?.firstOrNull()

            is QueryStrategy.RegionCode,
            is QueryStrategy.SearchTerm,
            -> null
        }

    /**
     * Returns every region code matching the given strategy.
     *
     * Supported strategies are [QueryStrategy.CallingCode] and
     * [QueryStrategy.RegionTitle].
     *
     * @param strategy The criterion by which to find the region
     *   codes.
     *
     * @return The matching region codes; otherwise, `null` if no
     *   match exists or the strategy is unsupported.
     */
    fun regionCodes(strategy: QueryStrategy): List<String>? =
        when (strategy) {
            is QueryStrategy.CallingCode -> regionCodesForCallingCode(callingCode = strategy.callingCode)

            is QueryStrategy.RegionTitle -> regionCodesForRegionTitle(regionTitle = strategy.regionTitle)

            is QueryStrategy.RegionCode,
            is QueryStrategy.SearchTerm,
            -> null
        }

    private fun regionCodesForCallingCode(callingCode: String): List<String>? {
        if (!callingCodes.values.contains(callingCode)) return null
        return callingCodes.filterValues { it == callingCode }.keys.toList()
    }

    private fun regionCodesForRegionTitle(regionTitle: String): List<String> {
        val format = titleFormat(regionTitle)
        return callingCodes.keys.filter {
            regionTitles(QueryStrategy.RegionCode(it), titleFormat = format)?.firstOrNull() == regionTitle
        }
    }

    // MARK: - Region Titles

    /**
     * Returns the localized display name for the given region code.
     *
     * @param regionCode The region code whose name to resolve.
     * @param languageCode The language code in which to localize the
     *   name. Pass `null` to use the app's language; results
     *   localized to the app's language are cached in memory. The
     *   default is `null`.
     *
     * @return The localized region name. If the region code is
     *   unknown, it is returned unchanged; if no localized name
     *   exists, a localized "Multiple" placeholder is returned.
     */
    fun localizedRegionName(
        regionCode: String,
        languageCode: String? = null,
    ): String {
        if (languageCode == null) {
            cachedLocalizedRegionNamesForRegionCodes.wrappedValue?.get(regionCode)?.let { return it }
        }

        if (callingCodes[regionCode] == null) return regionCode

        val locale = languageCode?.let { Locale(it) } ?: systemLocalizedLocale
        val regionName =
            locale.localizedString(regionCode = regionCode.uppercase())
                ?: LocalizedStringKey.Multiple.localized()

        if (languageCode == null) cachedLocalizedRegionNamesForRegionCodes.insert(regionCode to regionName)
        return regionName
    }

    /**
     * Returns the region titles matching the given strategy.
     *
     * Supported strategies are [QueryStrategy.CallingCode],
     * [QueryStrategy.RegionCode], and [QueryStrategy.SearchTerm] – a
     * blank search term matches every region title.
     *
     * @param strategy The criterion by which to find the region
     *   titles.
     * @param titleFormat The arrangement of each title's components.
     *   The default is [RegionTitleFormat.CALLING_CODE_FIRST].
     *
     * @return The matching region titles; otherwise, `null` if no
     *   match exists or the strategy is unsupported.
     */
    fun regionTitles(
        strategy: QueryStrategy,
        titleFormat: RegionTitleFormat = RegionTitleFormat.CALLING_CODE_FIRST,
    ): List<String>? =
        when (strategy) {
            is QueryStrategy.CallingCode ->
                regionTitleForCallingCode(
                    callingCode = strategy.callingCode,
                    titleFormat = titleFormat,
                )?.let { listOf(it) }

            is QueryStrategy.RegionCode ->
                regionTitleForRegionCode(
                    regionCode = strategy.regionCode,
                    titleFormat = titleFormat,
                )?.let { listOf(it) }

            is QueryStrategy.SearchTerm -> {
                if (strategy.searchTerm.isBlank()) {
                    getRegionTitlesForAllCallingCodes()
                } else {
                    val searchTerm = strategy.searchTerm.lowercase().trim()
                    getRegionTitlesForAllCallingCodes()
                        .filter { it.lowercase().trim().contains(searchTerm) }
                        .ifEmpty { null }
                }
            }

            is QueryStrategy.RegionTitle -> null
        }

    private fun getRegionTitlesForAllCallingCodes(): List<String> {
        cachedRegionTitlesForAllCallingCodes.wrappedValue?.takeIf { it.isNotEmpty() }?.let { return it }

        val titles =
            callingCodes.keys
                .mapNotNull { regionTitleForRegionCode(regionCode = it, titleFormat = RegionTitleFormat.REGION_NAME_FIRST) }
                .sorted()

        cachedRegionTitlesForAllCallingCodes.wrappedValue = titles
        return titles
    }

    private fun regionTitleForCallingCode(
        callingCode: String,
        titleFormat: RegionTitleFormat,
    ): String? {
        cachedRegionTitlesForCallingCodes.wrappedValue?.get(callingCode)?.let { (title, format) ->
            if (format == titleFormat) return title
        }

        if (!callingCodes.values.contains(callingCode)) return null
        val regions = callingCodes.filterValues { it == callingCode }.keys.toList()

        if (regions.size != 1) {
            val title = "+$callingCode (Multiple)"
            cachedRegionTitlesForCallingCodes.insert(callingCode to (title to titleFormat))
            return title
        }

        val title = regionTitles(QueryStrategy.RegionCode(regions[0]), titleFormat = titleFormat)?.firstOrNull() ?: return null
        cachedRegionTitlesForCallingCodes.insert(callingCode to (title to titleFormat))
        return title
    }

    private fun regionTitleForRegionCode(
        regionCode: String,
        titleFormat: RegionTitleFormat,
    ): String? {
        cachedRegionTitlesForRegionCodes.wrappedValue?.get(regionCode)?.let { (title, format) ->
            if (format == titleFormat) return title
        }

        val callingCode = callingCodes[regionCode] ?: return ""

        fun title(regionName: String): String {
            val title =
                when (titleFormat) {
                    RegionTitleFormat.CALLING_CODE_FIRST -> "+$callingCode ($regionName)"
                    RegionTitleFormat.REGION_NAME_FIRST -> "$regionName (+$callingCode)"
                }

            cachedRegionTitlesForRegionCodes.insert(regionCode to (title to titleFormat))
            return title
        }

        val regionName =
            systemLocalizedLocale.localizedString(regionCode = regionCode)
                ?: return title(LocalizedStringKey.Multiple.localized())

        return title(regionName)
    }

    // MARK: - Clear Cache

    /** Removes every cached lookup result. */
    fun clearCache() {
        cachedImagesForRegionCodes.wrappedValue = null
        cachedImagesForRegionTitles.wrappedValue = null
        cachedLocalizedRegionNamesForRegionCodes.wrappedValue = null
        cachedRegionTitlesForAllCallingCodes.wrappedValue = null
        cachedRegionTitlesForCallingCodes.wrappedValue = null
        cachedRegionTitlesForRegionCodes.wrappedValue = null
    }

    // MARK: - Auxiliary

    private fun <Key, Value> LockIsolated<Map<Key, Value>?>.insert(entry: Pair<Key, Value>) {
        withValue { it.value = (it.value ?: emptyMap()) + entry }
    }

    private fun titleFormat(regionTitle: String): RegionTitleFormat =
        if (regionTitle.startsWith("+")) RegionTitleFormat.CALLING_CODE_FIRST else RegionTitleFormat.REGION_NAME_FIRST
}

private const val FALLBACK_REGION_CODE = "US"
