//
//  Persistent.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

/**
 * Values that persist across app launches, keyed by
 * [PersistentStorageKey].
 *
 * Scalar values are stored in [android.content.SharedPreferences].
 * Larger collections are stored as archives under the file store,
 * one JSON file per key. [initialize] must be called once with the
 * application context before use.
 *
 * **Note:** device-identifying secrets use encrypted storage
 * instead; see `DeviceID` in the networking module.
 */
object Persistent {
    // MARK: - Properties

    private const val PREFERENCES_NAME = "persistent"

    @Volatile
    private var appContext: Context? = null

    private var testScalars: LockIsolated<Map<String, Any?>>? = null

    // MARK: - Initialization

    /** Prepares persistent storage for use. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /** Prepares scalar storage with an in-memory backend for tests. */
    fun initializeForTesting() {
        testScalars = LockIsolated(mapOf())
    }

    // MARK: - Scalar Accessors

    /** The stored string for [key], or `null`. */
    fun string(key: PersistentStorageKey): String? {
        testScalars?.let { return it.wrappedValue[key.rawValue] as? String }
        return preferences()?.getString(key.rawValue, null)
    }

    /** Stores [value] for [key], removing the entry when `null`. */
    fun setString(
        key: PersistentStorageKey,
        value: String?,
    ) {
        if (setTestScalar(key, value)) return
        val editor = preferences()?.edit() ?: return
        if (value == null) editor.remove(key.rawValue) else editor.putString(key.rawValue, value)
        editor.apply()
    }

    /** The stored boolean for [key], or [default] if unset. */
    fun boolean(
        key: PersistentStorageKey,
        default: Boolean = false,
    ): Boolean {
        testScalars?.let { return it.wrappedValue[key.rawValue] as? Boolean ?: default }
        return preferences()?.getBoolean(key.rawValue, default) ?: default
    }

    /** Stores [value] for [key]. */
    fun setBoolean(
        key: PersistentStorageKey,
        value: Boolean,
    ) {
        if (setTestScalar(key, value)) return
        preferences()?.edit()?.putBoolean(key.rawValue, value)?.apply()
    }

    /** The stored integer for [key], or `null` if unset. */
    fun int(key: PersistentStorageKey): Int? {
        testScalars?.let { return it.wrappedValue[key.rawValue] as? Int }
        val preferences = preferences() ?: return null
        return if (preferences.contains(key.rawValue)) preferences.getInt(key.rawValue, 0) else null
    }

    /** Stores [value] for [key], removing the entry when `null`. */
    fun setInt(
        key: PersistentStorageKey,
        value: Int?,
    ) {
        if (setTestScalar(key, value)) return
        val editor = preferences()?.edit() ?: return
        if (value == null) editor.remove(key.rawValue) else editor.putInt(key.rawValue, value)
        editor.apply()
    }

    /** The stored long for [key], or `null` if unset. */
    fun long(key: PersistentStorageKey): Long? {
        testScalars?.let { return it.wrappedValue[key.rawValue] as? Long }
        val preferences = preferences() ?: return null
        return if (preferences.contains(key.rawValue)) preferences.getLong(key.rawValue, 0L) else null
    }

    /** Stores [value] for [key], removing the entry when `null`. */
    fun setLong(
        key: PersistentStorageKey,
        value: Long?,
    ) {
        if (setTestScalar(key, value)) return
        val editor = preferences()?.edit() ?: return
        if (value == null) editor.remove(key.rawValue) else editor.putLong(key.rawValue, value)
        editor.apply()
    }

    // MARK: - Archive Accessors

    /**
     * Returns the archived value for [key], decoded from its stored
     * encoded maps, or `null` if no archive exists.
     *
     * @param key The key identifying the archive.
     * @param decode A closure that reconstructs the value from the
     *   stored list of encoded maps.
     */
    fun <T> archive(
        key: PersistentStorageKey,
        decode: (List<Map<String, Any?>>) -> T,
    ): T? {
        val file = FileStore.resolve(archivePath(key)) ?: return null
        if (!file.exists()) return null
        val json = runCatching { file.readText() }.getOrNull() ?: return null
        val maps = runCatching { decodeArchive(json) }.getOrNull() ?: return null
        return decode(maps)
    }

    /**
     * Stores [encoded] as the archive for [key], removing the
     * archive when `null`.
     *
     * The archive is written atomically to a JSON file under the
     * file store.
     *
     * @param key The key identifying the archive.
     * @param encoded The list of encoded maps to store, or `null`
     *   to remove the archive.
     */
    fun setArchive(
        key: PersistentStorageKey,
        encoded: List<Map<String, Any?>>?,
    ) {
        val file = FileStore.resolve(archivePath(key)) ?: return
        if (encoded == null) {
            file.delete()
            return
        }

        file.parentFile?.mkdirs()
        val temporaryFile = java.io.File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(encodeArchive(encoded))
        if (file.exists()) file.delete()
        temporaryFile.renameTo(file)
    }

    // MARK: - Reset

    /**
     * Removes every persisted value, preserving only the entries for
     * the given keys.
     *
     * Both scalar values and archives are cleared.
     *
     * @param preserving The keys whose values survive the reset.
     */
    fun reset(preserving: List<PersistentStorageKey>) {
        val preservedRawValues = preserving.map { it.rawValue }.toSet()

        testScalars?.let { store ->
            store.withValue { it.value = it.value.filterKeys { key -> key in preservedRawValues } }
        } ?: run {
            val preferences = preferences() ?: return@run
            val editor = preferences.edit()
            preferences.all.keys
                .filter { it !in preservedRawValues }
                .forEach { editor.remove(it) }
            editor.apply()
        }

        val preservedArchives =
            preserving.mapNotNull { key ->
                val file = FileStore.resolve(archivePath(key))
                if (file != null && file.exists()) key.rawValue to file.readText() else null
            }

        FileStore.delete("persistent")
        preservedArchives.forEach { (rawValue, json) ->
            val file = FileStore.resolve("persistent/$rawValue.json") ?: return@forEach
            file.parentFile?.mkdirs()
            file.writeText(json)
        }
    }

    /**
     * Returns the keys preserved across a reset: the permanent keys,
     * the subsystem keys, and any additional keys.
     *
     * @param plus Additional keys to preserve, or `null` for none.
     */
    fun permanentAndSubsystemKeys(plus: List<PersistentStorageKey>? = null): List<PersistentStorageKey> {
        val permanentKeys = AppSubsystem.delegates.permanentPersistentStorageKeys?.permanentKeys ?: emptyList()
        return ((plus ?: emptyList()) + permanentKeys + PersistentStorageKey.subsystemKeys).distinct()
    }

    // MARK: - Auxiliary

    private fun archivePath(key: PersistentStorageKey): String = "persistent/${key.rawValue}.json"

    private fun decodeArchive(json: String): List<Map<String, Any?>> {
        val array = Json.parseToJsonElement(json) as? JsonArray ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return array.mapNotNull { fromJsonElement(it) as? Map<String, Any?> }
    }

    private fun encodeArchive(maps: List<Map<String, Any?>>): String =
        JsonArray(maps.map { toJsonElement(it) }).toString()

    private fun fromJsonElement(element: JsonElement): Any? =
        when (element) {
            is JsonNull -> null
            is JsonObject -> element.mapValues { fromJsonElement(it.value) }
            is JsonArray -> element.map { fromJsonElement(it) }
            is JsonPrimitive ->
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.doubleOrNull
                    else -> element.content
                }
        }

    private fun preferences() = appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun setTestScalar(
        key: PersistentStorageKey,
        value: Any?,
    ): Boolean {
        val store = testScalars ?: return false
        store.withValue {
            it.value = if (value == null) it.value - key.rawValue else it.value + (key.rawValue to value)
        }
        return true
    }

    private fun toJsonElement(value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is JsonElement -> value
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Map<*, *> -> JsonObject(value.entries.associate { (key, entry) -> key.toString() to toJsonElement(entry) })
            is List<*> -> JsonArray(value.map { toJsonElement(it) })
            is Array<*> -> JsonArray(value.map { toJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
}
