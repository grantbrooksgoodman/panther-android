//
//  DevModeAppActionDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.interfaces

import us.neotechnica.panther.designsystem.modules.developermode.models.DevModeAction

/**
 * A type that supplies app-specific actions to the Developer Mode menu.
 *
 * Conform to [DevModeAppActionDelegate] and return the actions your app
 * needs in [appActions]. The Developer Mode menu loads these actions
 * when its action sheet is presented.
 *
 * Register the conforming instance through
 * [DevModeService.registerAppActionDelegate][us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService.registerAppActionDelegate]
 * during app setup so that it is available when the menu opens.
 */
interface DevModeAppActionDelegate {
    /** The actions to display in the app domain of the Developer Mode menu. */
    val appActions: List<DevModeAction>
}
