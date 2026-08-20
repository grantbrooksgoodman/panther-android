//
//  SharedEvents+ThemingExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/**
 * An event that fires whenever the active theme or appearance changes,
 * so that themed views update with the new colors.
 */
val SharedEvents.themedViewAppearanceChanged: EventStream<Unit>
    get() = event("themedViewAppearanceChanged")
