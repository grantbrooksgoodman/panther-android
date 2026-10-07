//
//  SharedEvents+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/** An event that requests dismissal of the keyboard, resigning any active first responder. */
val SharedEvents.resignFirstResponders: EventStream<Unit>
    get() = event("resignFirstResponders")
