//
//  BuildInfoOverlayDotIndicatorColorDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.interfaces

import androidx.compose.ui.graphics.Color

/**
 * A type that provides the color for the Developer Mode indicator
 * dot in the build info overlay.
 *
 * When Developer Mode is enabled, the build info overlay displays
 * a small colored dot to signal that the mode is active. By
 * default, this dot is orange. Conform to this interface to
 * supply a custom color that better suits your app's visual
 * design:
 *
 * ```kotlin
 * object AppBuildInfoDotColor : BuildInfoOverlayDotIndicatorColorDelegate {
 *     override val developerModeIndicatorDotColor: Color
 *         get() = Color.Cyan
 * }
 * ```
 *
 * Register your conformance through
 * `AppSubsystem.delegates.registerBuildInfoOverlayDotIndicatorColorDelegate`.
 */
interface BuildInfoOverlayDotIndicatorColorDelegate {
    /** The color to use for the Developer Mode indicator dot. */
    val developerModeIndicatorDotColor: Color
}
