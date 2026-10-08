//
//  RadioTechnology.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.models

import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import us.neotechnica.panther.networking.Networking

// A coarse classification of the device's current cellular radio
// access technology. The classification is a prior, not a
// measurement – it caps the health score on legacy cellular
// technologies where a starved estimator would otherwise
// over-report. Unavailable or unrecognized technologies –
// including future ones – classify as UNKNOWN and have no effect
// on scoring. Below API level 33 the type always reports UNKNOWN.
internal enum class RadioTechnology(
    val rawValue: String,
) {
    // 3G-class technologies (WCDMA, HSPA, EV-DO, eHRPD).
    INTERMEDIATE("intermediate"),

    // 2G-class technologies (GPRS, EDGE, CDMA 1x).
    LEGACY("legacy"),

    // 4G- and 5G-class technologies (LTE, NR).
    MODERN("modern"),

    // The technology is unavailable or unrecognized.
    UNKNOWN("unknown"),
    ;

    // MARK: - Companion

    companion object {
        // The radio access technology currently reported for the
        // device's data connection.
        val current: RadioTechnology
            get() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return UNKNOWN
                return runCatching { fromDataNetworkType() }.getOrDefault(UNKNOWN)
            }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun fromDataNetworkType(): RadioTechnology {
            val telephonyManager =
                Networking
                    .requireContext()
                    .getSystemService(TelephonyManager::class.java)

            return when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_GSM,
                TelephonyManager.NETWORK_TYPE_IDEN,
                -> LEGACY

                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                TelephonyManager.NETWORK_TYPE_UMTS,
                -> INTERMEDIATE

                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyManager.NETWORK_TYPE_NR,
                -> MODERN

                else -> UNKNOWN
            }
        }
    }
}
