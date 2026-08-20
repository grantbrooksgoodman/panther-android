//
//  DeviceID.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.user.models

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import us.neotechnica.panther.networking.Networking
import java.util.UUID

/**
 * A stable, per-install device identifier.
 *
 * The iOS original persists `identifierForVendor` in the Keychain.
 * Android has no vendor identifier, so this port persists a random
 * UUID in [EncryptedSharedPreferences] – the Keychain analog. The
 * value survives app restarts but resets on reinstall, which the
 * user-parity acceptance criterion explicitly permits.
 */
object DeviceID {
    // MARK: - Constants

    private const val PREFERENCES_NAME = "device_id"
    private const val KEY = "us.neotechnica.deviceID"

    // MARK: - Computed Properties

    /** The current device identifier, generating and persisting one if needed. */
    val current: String
        get() {
            val preferences = encryptedPreferences()
            preferences?.getString(KEY, null)?.let { return it }

            val newID = UUID.randomUUID().toString()
            preferences?.edit()?.putString(KEY, newID)?.apply()
            return newID
        }

    // MARK: - Auxiliary

    private fun encryptedPreferences(): SharedPreferences? {
        val context = Networking.requireContext()
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
