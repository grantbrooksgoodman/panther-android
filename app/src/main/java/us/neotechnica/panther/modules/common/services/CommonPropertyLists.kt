//
//  CommonPropertyLists.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import org.json.JSONObject

/**
 * Loads the bundled calling-code and number-length lookup tables,
 * ported from the iOS `CallingCodes.plist` and `LookupTables.plist`.
 *
 * [initialize] must be called once with the application context before
 * the tables are read. Parsed tables are cached in memory.
 */
object CommonPropertyLists {
    // MARK: - Properties

    @Volatile
    private var appContext: Context? = null

    private val cachedCallingCodes: Map<String, String> by lazy { loadCallingCodes() }
    private val cachedLookupTables: Map<String, List<String>> by lazy { loadLookupTables() }

    // MARK: - Computed Properties

    /** A map of region code to calling code, e.g. `"US" -> "1"`. */
    val callingCodes: Map<String, String> get() = cachedCallingCodes

    /** A map of national-number length to the calling codes of that length. */
    val lookupTables: Map<String, List<String>> get() = cachedLookupTables

    // MARK: - Initialization

    /** Prepares the property lists for use. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Auxiliary

    private fun loadCallingCodes(): Map<String, String> {
        val json = readAsset("propertylists/calling_codes.json") ?: return emptyMap()
        val root = JSONObject(json)
        val result = HashMap<String, String>(root.length())
        for (key in root.keys()) result[key] = root.getString(key)
        return result
    }

    private fun loadLookupTables(): Map<String, List<String>> {
        val json = readAsset("propertylists/lookup_tables.json") ?: return emptyMap()
        val root = JSONObject(json)
        val result = HashMap<String, List<String>>(root.length())
        for (key in root.keys()) {
            val array = root.getJSONArray(key)
            result[key] = List(array.length()) { array.getString(it) }
        }
        return result
    }

    private fun readAsset(name: String): String? =
        appContext
            ?.assets
            ?.open(name)
            ?.bufferedReader()
            ?.use { it.readText() }
}
