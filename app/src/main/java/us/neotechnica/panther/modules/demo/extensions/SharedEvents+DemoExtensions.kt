//
//  SharedEvents+DemoExtensions.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.demo.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/**
 * A demonstration event that signals a ping request.
 *
 * Part of the Phase 1 kernel demo; removed once real features
 * land.
 */
val SharedEvents.demoPingRequested: EventStream<Unit>
    get() = event("demoPingRequested")
