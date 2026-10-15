//
//  AppCheck.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// The App Check provider factory that release builds install. Debug
// builds install the App Check debug provider instead (src/debug),
// which never ships in a release build.
internal val appCheckProviderFactory: AppCheckProviderFactory
    get() = PlayIntegrityAppCheckProviderFactory.getInstance()
