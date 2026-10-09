//
//  SharedEvents+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/** An event that requests dismissal of the keyboard, resigning any active first responder. */
val SharedEvents.resignFirstResponders: EventStream<Unit>
    get() = event("resignFirstResponders")

/**
 * An event that fires whenever the user touches the root view in
 * a prerelease build.
 */
val SharedEvents.rootViewTapped: EventStream<Unit>
    get() = event("rootViewTapped")
