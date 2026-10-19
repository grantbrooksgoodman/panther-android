//
//  DeviceID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.sources.user.models

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.UUID

/**
 * A namespace for the current device's persistent identifier.
 */
object DeviceID {
    // MARK: - Types

    /** The identifier store and the platform identifier source. */
    internal data class Dependencies(
        val androidID: () -> String? = DeviceID::androidID,
        val read: () -> String? = DeviceID::read,
        val save: (String) -> Unit = DeviceID::save,
    )

    // MARK: - Properties

    /**
     * The prefix every identifier minted on this platform
     * carries.
     */
    const val PREFIX = "and_"

    private const val ACCOUNT = "us.neotechnica.deviceID"
    private const val BACKUP_PREFERENCES_NAME = "device_id_backup"
    private const val PREFERENCES_NAME = "device_id"

    private val cachedID = LockIsolated<String?>(null)
    private var appContext: Context? = null
    private var dependencies = Dependencies()

    // MARK: - Computed Properties

    /**
     * The current device's identifier.
     *
     * A persisted identifier is returned verbatim. Otherwise one is
     * minted with the platform prefix from the device's Android ID,
     * or a new UUID when unavailable, persisted to encrypted
     * preferences, mirrored to a plain-preferences backup, and cached
     * in process.
     */
    val current: String
        get() =
            cachedID.withValue { reference ->
                reference.value?.let { return@withValue it }

                dependencies.read()?.let { existingID ->
                    reference.value = existingID
                    return@withValue existingID
                }

                val newID = "$PREFIX${dependencies.androidID() ?: UUID.randomUUID()}"
                dependencies.save(newID)
                reference.value = newID
                newID
            }

    // MARK: - Methods

    /** Prepares the identifier store with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Replaces the store and source and forgets the cached
     * identifier.
     */
    internal fun setDependencies(dependencies: Dependencies) {
        this.dependencies = dependencies
        cachedID.wrappedValue = null
    }

    // MARK: - Auxiliary

    private fun androidID(): String? {
        val context = appContext ?: return null
        return runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun backupPreferences(): SharedPreferences? = appContext?.getSharedPreferences(BACKUP_PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun encryptedPreferences(): SharedPreferences? {
        val context = appContext ?: return null
        return try {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            EncryptedSharedPreferences.create(
                context,
                PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun read(): String? = encryptedPreferences()?.getString(ACCOUNT, null) ?: backupPreferences()?.getString(ACCOUNT, null)

    private fun save(value: String) {
        encryptedPreferences()?.edit()?.putString(ACCOUNT, value)?.apply()
        backupPreferences()?.edit()?.putString(ACCOUNT, value)?.apply()
    }
}
