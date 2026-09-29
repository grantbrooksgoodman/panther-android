//
//  Build+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 27/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.modules.common.services.ConnectionStatusService
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * A Boolean value that indicates whether the current build is running
 * on an emulator.
 *
 * The Android analog of the iOS `UIDevice.isSimulator` check.
 */
val Build.isEmulator: Boolean
    get() =
        android.os.Build.FINGERPRINT.startsWith("generic") ||
            android.os.Build.FINGERPRINT.startsWith("unknown") ||
            android.os.Build.MODEL.contains("google_sdk") ||
            android.os.Build.MODEL.contains("Emulator") ||
            android.os.Build.MODEL.contains("Android SDK built for") ||
            android.os.Build.MANUFACTURER.contains("Genymotion") ||
            android.os.Build.HARDWARE.contains("goldfish") ||
            android.os.Build.HARDWARE.contains("ranchu") ||
            android.os.Build.PRODUCT.contains("sdk")

/**
 * A Boolean value that indicates whether developer mode is enabled.
 *
 * Developer-mode affordances – such as the conversations page's
 * delete-conversations toolbar button – are shown only when this is
 * `true`. The persisted developer-mode toggle that drives it arrives
 * with the settings port.
 */
val Build.isDeveloperModeEnabled: Boolean
    get() = false

/** A Boolean value that indicates whether the device is online. */
val Build.isOnline: Boolean
    get() = ConnectionStatusService.isOnline
