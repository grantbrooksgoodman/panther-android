//
//  CommonPropertyLists.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.content.Context
import org.json.JSONObject
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * Reads phone number reference data bundled with the app.
 *
 * Loaded values are cached in memory. Call [initialize] once with
 * the application context before the values are read.
 */
object CommonPropertyLists {
    // MARK: - Properties

    private val cachedCallingCodes = LockIsolated<Map<String, String>?>(null)
    private val cachedLookupTables = LockIsolated<Map<String, List<String>>?>(null)

    @Volatile
    private var appContext: Context? = null

    // MARK: - Computed Properties

    /**
     * A dictionary that maps region codes to their international
     * calling codes.
     *
     * The dictionary is loaded from the bundled calling-code
     * resource and cached in memory. If the resource cannot be
     * loaded, this property is an empty dictionary.
     */
    val callingCodes: Map<String, String>
        get() {
            cachedCallingCodes.wrappedValue?.takeIf { it.isNotEmpty() }?.let { return it }
            val dictionary = runCatching { loadCallingCodes() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return emptyMap()
            cachedCallingCodes.wrappedValue = dictionary
            return dictionary
        }

    /**
     * A dictionary that maps national phone number lengths, as
     * strings, to the calling codes whose numbers have that length.
     *
     * The dictionary is loaded from the bundled lookup-table
     * resource and cached in memory. If the resource cannot be
     * loaded, this property is an empty dictionary.
     */
    val lookupTables: Map<String, List<String>>
        get() {
            cachedLookupTables.wrappedValue?.takeIf { it.isNotEmpty() }?.let { return it }
            val dictionary = runCatching { loadLookupTables() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return emptyMap()
            cachedLookupTables.wrappedValue = dictionary
            return dictionary
        }

    // MARK: - Initialization

    /** Prepares the property lists for use. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    internal fun initializeForTesting(
        callingCodes: Map<String, String>,
        lookupTables: Map<String, List<String>>,
    ) {
        cachedCallingCodes.wrappedValue = callingCodes
        cachedLookupTables.wrappedValue = lookupTables
    }

    // MARK: - Clear Cache

    /** Removes every cached property list value. */
    fun clearCache() {
        cachedCallingCodes.wrappedValue = null
        cachedLookupTables.wrappedValue = null
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
