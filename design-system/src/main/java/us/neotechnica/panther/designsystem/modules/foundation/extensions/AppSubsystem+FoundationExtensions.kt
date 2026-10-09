//
//  AppSubsystem+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import us.neotechnica.panther.designsystem.modules.foundation.interfaces.BuildInfoOverlayDotIndicatorColorDelegate
import us.neotechnica.panther.subsystem.AppSubsystem
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

// MARK: - Properties

private val storedBuildInfoOverlayDotIndicatorColor = LockIsolated<BuildInfoOverlayDotIndicatorColorDelegate?>(null)

// MARK: - Computed Properties

/**
 * The delegate that provides a custom color for the Developer
 * Mode indicator dot in the build info overlay.
 *
 * When this property is `null`, the indicator dot uses the
 * default color.
 */
val AppSubsystem.Delegates.buildInfoOverlayDotIndicatorColor: BuildInfoOverlayDotIndicatorColorDelegate?
    get() = storedBuildInfoOverlayDotIndicatorColor.wrappedValue

// MARK: - Methods

/**
 * Registers the specified build info overlay dot indicator color
 * delegate.
 *
 * @param buildInfoOverlayDotIndicatorColorDelegate The delegate to
 *   register.
 */
fun AppSubsystem.Delegates.registerBuildInfoOverlayDotIndicatorColorDelegate(
    buildInfoOverlayDotIndicatorColorDelegate: BuildInfoOverlayDotIndicatorColorDelegate,
) {
    storedBuildInfoOverlayDotIndicatorColor.wrappedValue = buildInfoOverlayDotIndicatorColorDelegate
}
