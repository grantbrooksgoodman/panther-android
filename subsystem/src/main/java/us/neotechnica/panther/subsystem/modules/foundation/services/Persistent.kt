//
//  Persistent.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

    /** The stored boolean for [key], or `null` if unset. */
    fun booleanOrNull(key: PersistentStorageKey): Boolean? {
        testScalars?.let { return it.wrappedValue[key.rawValue] as? Boolean }
        val preferences = preferences() ?: return null
        return if (preferences.contains(key.rawValue)) preferences.getBoolean(key.rawValue, false) else null
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
    ): T? = archiveElements(key) { it }?.let(decode)

    /**
     * Returns the elements of the archive for [key], each decoded
     * from its stored encoded map, or `null` if no archive exists
     * or it cannot be read.
     *
     * The archive is read one element at a time, so memory use
     * follows the decoded elements rather than the archive's full
     * text. Elements for which [decode] returns `null` are skipped.
     *
     * @param key The key identifying the archive.
     * @param decode A closure that reconstructs an element from its
     *   stored encoded map, or returns `null` to skip it.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun <T : Any> archiveElements(
        key: PersistentStorageKey,
        decode: (Map<String, Any?>) -> T?,
    ): List<T>? {
        val file = FileStore.resolve(archivePath(key)) ?: return null
        if (!file.exists()) return null
        return runCatching {
            file.inputStream().buffered().use { stream ->
                Json
                    .decodeToSequence(
                        stream,
                        JsonElement.serializer(),
                        DecodeSequenceMode.ARRAY_WRAPPED,
                    ).mapNotNull { element ->
                        @Suppress("UNCHECKED_CAST")
                        (fromJsonElement(element) as? Map<String, Any?>)?.let(decode)
                    }.toList()
            }
        }.getOrNull()
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
        setArchive(key, encoded) { it }
    }

    /**
     * Stores [values] as the archive for [key], each encoded to a
     * map by [encode] as it is written, removing the archive when
     * `null`.
     *
     * The archive is written to a temporary file one element at a
     * time, then moved over the stored archive in a single step.
     * Memory use therefore follows a single element rather than the
     * archive's full text, and an interrupted write leaves the
     * previous archive in place.
     *
     * @param key The key identifying the archive.
     * @param values The values to store, or `null` to remove the
     *   archive.
     * @param encode A closure that encodes a value to the map stored
     *   for it.
     */
    fun <T> setArchive(
        key: PersistentStorageKey,
        values: Collection<T>?,
        encode: (T) -> Map<String, Any?>,
    ) {
        val file = FileStore.resolve(archivePath(key)) ?: return
        if (values == null) {
            file.delete()
            return
        }

        file.parentFile?.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.bufferedWriter().use { writer ->
            writer.write("[")
            values.forEachIndexed { index, value ->
                if (index > 0) writer.write(",")
                writer.write(toJsonElement(encode(value)).toString())
            }

            writer.write("]")
        }

        Files.move(
            temporaryFile.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    // MARK: - Reset

    /**
     * A rule that determines which persisted keys survive a reset.
     *
     * Pass a strategy to [reset] to control which entries are
     * retained – for example,
     * `KeyPreservationStrategy.PermanentAndSubsystemKeys(plus = listOf(key))`.
     */
    sealed interface KeyPreservationStrategy {
        /** Preserve only the specified keys. */
        data class Custom(
            val keys: List<PersistentStorageKey>,
        ) : KeyPreservationStrategy

        /** Preserve nothing; all keys are removed. */
        data object None : KeyPreservationStrategy

        /**
         * Preserve the permanent keys registered through the
         * [PermanentPersistentStorageKeyDelegate][us.neotechnica.panther.subsystem.modules.foundation.interfaces.PermanentPersistentStorageKeyDelegate],
         * the subsystem keys, and any additional keys.
         */
        data class PermanentAndSubsystemKeys(
            val plus: List<PersistentStorageKey>? = null,
        ) : KeyPreservationStrategy

        /** Preserve the subsystem keys and any additional keys. */
        data class SubsystemKeys(
            val plus: List<PersistentStorageKey>? = null,
        ) : KeyPreservationStrategy
    }

    /**
     * Removes every persisted value, preserving only the entries
     * selected by the given strategy.
     *
     * Both scalar values and archives are cleared. The default
     * strategy preserves the permanent keys registered through the
     * [PermanentPersistentStorageKeyDelegate][us.neotechnica.panther.subsystem.modules.foundation.interfaces.PermanentPersistentStorageKeyDelegate]
     * and the subsystem keys.
     *
     * @param preserving The preservation strategy. Defaults to
     *   [KeyPreservationStrategy.PermanentAndSubsystemKeys].
     */
    fun reset(preserving: KeyPreservationStrategy = KeyPreservationStrategy.PermanentAndSubsystemKeys()) {
        val preservedKeys = resolveKeys(preserving)
        val preservedRawValues = preservedKeys.map { it.rawValue }.toSet()

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
            preservedKeys.mapNotNull { key ->
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

    private fun resolveKeys(strategy: KeyPreservationStrategy): List<PersistentStorageKey> =
        when (strategy) {
            is KeyPreservationStrategy.Custom -> strategy.keys.distinct()

            KeyPreservationStrategy.None -> emptyList()

            is KeyPreservationStrategy.PermanentAndSubsystemKeys -> {
                val permanentKeys = AppSubsystem.delegates.permanentPersistentStorageKeys?.permanentKeys ?: emptyList()
                ((strategy.plus ?: emptyList()) + permanentKeys + PersistentStorageKey.subsystemKeys).distinct()
            }

            is KeyPreservationStrategy.SubsystemKeys ->
                ((strategy.plus ?: emptyList()) + PersistentStorageKey.subsystemKeys).distinct()
        }

    // MARK: - Auxiliary

    private fun archivePath(key: PersistentStorageKey): String = "persistent/${key.rawValue}.json"

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
