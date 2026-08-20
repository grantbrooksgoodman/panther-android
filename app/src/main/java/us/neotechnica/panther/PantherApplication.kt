//
//  PantherApplication.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther

import android.app.Application
import us.neotechnica.panther.modules.localization.services.LocalizedStringResolver
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment

/**
 * The application entry point.
 *
 * Initializes the Networking framework with the environment
 * baked into the active build flavor and the App Check provider
 * appropriate to the build type, and prepares localization.
 */
class PantherApplication : Application() {
    // MARK: - Application

    override fun onCreate() {
        super.onCreate()

        LocalizedStringResolver.initialize(this)

        Networking.initialize(
            context = this,
            defaultEnvironment = NetworkEnvironment.from(BuildConfig.NETWORK_ENVIRONMENT),
            useDebugAppCheckProvider = BuildConfig.DEBUG,
        )
    }
}
