//
//  BuildInfoOverlayDotIndicatorColorDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.services

import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkEnvironment
import us.neotechnica.panther.designsystem.modules.foundation.interfaces.BuildInfoOverlayDotIndicatorColorDelegate as AppSubsystemBuildInfoOverlayDotIndicatorColorDelegate

/**
 * A delegate that maps the active network environment to a
 * colored dot on the build info overlay.
 *
 * Register this delegate with the app subsystem to display a
 * colored indicator that reflects the current
 * [NetworkEnvironment]:
 *
 * - **Development**: green
 * - **Staging**: orange
 * - **Production**: red
 */
object BuildInfoOverlayDotIndicatorColorDelegate : AppSubsystemBuildInfoOverlayDotIndicatorColorDelegate {
    // MARK: - Computed Properties

    /**
     * The color of the Developer Mode indicator dot, based on
     * the active network environment.
     */
    override val developerModeIndicatorDotColor: Color
        get() =
            when (Networking.config.environment) {
                NetworkEnvironment.DEVELOPMENT -> Color(0xFF34C759)
                NetworkEnvironment.PRODUCTION -> Color(0xFFFF3B30)
                NetworkEnvironment.STAGING -> Color(0xFFFF9500)
            }
}
