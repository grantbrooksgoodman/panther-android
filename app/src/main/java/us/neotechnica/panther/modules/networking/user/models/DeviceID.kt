//
//  DeviceID.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.user.models

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated
import java.util.UUID

/**
 * A stable, per-install device identifier.
 *
 * The identifier is a random UUID persisted in
 * [EncryptedSharedPreferences], mirrored to a plain-preferences
 * backup and cached in process so the value stays stable for the
 * lifetime of an install even when the encrypted store is
 * unavailable. The value survives app restarts but resets on
 * reinstall.
 */
object DeviceID {
    // MARK: - Constants

    private const val PREFERENCES_NAME = "device_id"
    private const val BACKUP_PREFERENCES_NAME = "device_id_backup"
    private const val KEY = "us.neotechnica.deviceID"

    // MARK: - Properties

    private var appContext: Context? = null
    private val cachedID = LockIsolated<String?>(null)

    // MARK: - Computed Properties

    /** The current device identifier, generating and persisting one if needed. */
    val current: String
        get() =
            cachedID.withValue { reference ->
                reference.value?.let { return@withValue it }

                val encrypted = encryptedPreferences()
                val backup = backupPreferences()

                val resolved =
                    encrypted?.getString(KEY, null)
                        ?: backup?.getString(KEY, null)
                        ?: UUID.randomUUID().toString()

                encrypted?.edit()?.putString(KEY, resolved)?.apply()
                backup?.edit()?.putString(KEY, resolved)?.apply()
                reference.value = resolved
                resolved
            }

    // MARK: - Methods

    /** Prepares the identifier store with the application context. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    // MARK: - Auxiliary

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
}
