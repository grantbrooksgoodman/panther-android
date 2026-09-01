//
//  Persistent.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import android.content.Context
import us.neotechnica.panther.subsystem.modules.foundation.models.PersistentStorageKey

/**
 * Values that persist across app launches, keyed by
 * [PersistentStorageKey].
 *
 * The Android analog of the iOS `@Persistent` property wrapper (which
 * is backed by `UserDefaults`), this is backed by regular
 * [android.content.SharedPreferences]. [initialize] must be called
 * once with the application context before use.
 *
 * **Note:** device-identifying secrets use encrypted storage instead;
 * see `DeviceID` in the networking module.
 */
object Persistent {
    // MARK: - Properties

    private const val PREFERENCES_NAME = "persistent"

    @Volatile
    private var appContext: Context? = null

    // MARK: - Initialization

    /** Prepares persistent storage for use. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Accessors

    /** The stored string for [key], or `null`. */
    fun string(key: PersistentStorageKey): String? = preferences()?.getString(key.rawValue, null)

    /** Stores [value] for [key], removing the entry when `null`. */
    fun setString(
        key: PersistentStorageKey,
        value: String?,
    ) {
        val editor = preferences()?.edit() ?: return
        if (value == null) editor.remove(key.rawValue) else editor.putString(key.rawValue, value)
        editor.apply()
    }

    /** The stored boolean for [key], or [default] if unset. */
    fun boolean(
        key: PersistentStorageKey,
        default: Boolean = false,
    ): Boolean = preferences()?.getBoolean(key.rawValue, default) ?: default

    /** Stores [value] for [key]. */
    fun setBoolean(
        key: PersistentStorageKey,
        value: Boolean,
    ) {
        preferences()?.edit()?.putBoolean(key.rawValue, value)?.apply()
    }

    /** The stored integer for [key], or `null` if unset. */
    fun int(key: PersistentStorageKey): Int? {
        val preferences = preferences() ?: return null
        return if (preferences.contains(key.rawValue)) preferences.getInt(key.rawValue, 0) else null
    }

    /** Stores [value] for [key], removing the entry when `null`. */
    fun setInt(
        key: PersistentStorageKey,
        value: Int?,
    ) {
        val editor = preferences()?.edit() ?: return
        if (value == null) editor.remove(key.rawValue) else editor.putInt(key.rawValue, value)
        editor.apply()
    }

    /** The stored long for [key], or `null` if unset. */
    fun long(key: PersistentStorageKey): Long? {
        val preferences = preferences() ?: return null
        return if (preferences.contains(key.rawValue)) preferences.getLong(key.rawValue, 0L) else null
    }

    /** Stores [value] for [key], removing the entry when `null`. */
    fun setLong(
        key: PersistentStorageKey,
        value: Long?,
    ) {
        val editor = preferences()?.edit() ?: return
        if (value == null) editor.remove(key.rawValue) else editor.putLong(key.rawValue, value)
        editor.apply()
    }

    // MARK: - Auxiliary

    private fun preferences() = appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
