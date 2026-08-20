//
//  RuntimeStorage.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import java.util.Locale

/**
 * A thread-safe, in-memory key-value store for data that should
 * persist for the lifetime of the current launch, but not across
 * launches.
 *
 * Use [RuntimeStorage] to share transient state – such as a
 * session token or a resolved configuration value – between
 * otherwise unrelated parts of the app. Values are stored by
 * [StoredItemKey] and are accessible from any thread:
 *
 * ```kotlin
 * // Store a value:
 * RuntimeStorage.store(session, CURRENT_SESSION_STORED_ITEM_KEY)
 *
 * // Retrieve it later:
 * val session = RuntimeStorage.retrieve(CURRENT_SESSION_STORED_ITEM_KEY) as? Session
 *
 * // Remove it when no longer needed:
 * RuntimeStorage.remove(CURRENT_SESSION_STORED_ITEM_KEY)
 * ```
 *
 * **Note:** [RuntimeStorage] is not a substitute for persistent
 * storage. Values are discarded when the process terminates.
 */
object RuntimeStorage {
    // MARK: - Properties

    private val storedItems = LockIsolated(mapOf<String, Any>())
    private val currentLanguageCode = LockIsolated(Locale.getDefault().language)

    // MARK: - Computed Properties

    /**
     * The ISO 639-1 code of the app's active language.
     *
     * Defaults to the device language; later phases set it from the
     * user's stored preference. This is the target of
     * [LanguagePair.system][us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage]
     * for display-string translation.
     */
    var languageCode: String
        get() = currentLanguageCode.wrappedValue
        set(value) {
            currentLanguageCode.wrappedValue = value
        }

    // MARK: - Methods

    /**
     * Removes the value associated with the given key.
     *
     * If no value is stored for the key, this method has no
     * effect.
     *
     * @param item The key whose value should be removed.
     */
    fun remove(item: StoredItemKey) {
        storedItems.withValue {
            it.value = it.value - item.rawValue
        }
    }

    /**
     * Returns the value associated with the given key, or `null`
     * if no value is stored.
     *
     * The returned value is untyped. Cast it to the expected
     * type at the call site.
     *
     * @param item The key to look up.
     *
     * @return The stored value, or `null` if the key is not
     *   present.
     */
    fun retrieve(item: StoredItemKey): Any? =
        storedItems.withValue {
            it.value[item.rawValue]
        }

    /**
     * Stores a value under the given key, replacing any existing
     * value.
     *
     * @param obj The value to store.
     * @param item The key to associate with the value.
     */
    fun store(
        obj: Any,
        item: StoredItemKey,
    ) {
        storedItems.withValue {
            it.value = it.value + (item.rawValue to obj)
        }
    }
}
