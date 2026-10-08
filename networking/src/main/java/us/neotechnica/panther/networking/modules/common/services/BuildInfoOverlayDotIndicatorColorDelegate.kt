//
//  BuildInfoOverlayDotIndicatorColorDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.services

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment

/**
 * A delegate that maps the active network environment to a
 * colored dot on the build info overlay.
 *
 * Register this delegate to display a colored indicator that
 * reflects the current [NetworkEnvironment]:
 *
 * - **Development**: green
 * - **Staging**: orange
 * - **Production**: red
 */
object BuildInfoOverlayDotIndicatorColorDelegate {
    // MARK: - Computed Properties

    /**
     * The color of the Developer Mode indicator dot, based on
     * the active network environment.
     */
    val developerModeIndicatorDotColor: Color
        get() =
            when (Networking.config.environment) {
                NetworkEnvironment.DEVELOPMENT -> Color.Green
                NetworkEnvironment.PRODUCTION -> Color.Red
                NetworkEnvironment.STAGING -> Color(0xFFFFA500)
            }
}
