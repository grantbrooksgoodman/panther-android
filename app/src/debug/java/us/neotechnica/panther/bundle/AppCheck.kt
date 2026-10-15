//
//  AppCheck.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// The App Check provider factory that debug builds install. The
// debug provider attests emulators and development devices, which
// Play Integrity can't; release builds install Play Integrity
// instead (src/release).
internal val appCheckProviderFactory: AppCheckProviderFactory
    get() = DebugAppCheckProviderFactory.getInstance()
