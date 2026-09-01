//
//  SharedStates+ForcedUpdateExtensions.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 01/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.StateStream

/**
 * A Boolean value that indicates whether a forced update is
 * required.
 *
 * Set live by the update service when the hosted metadata reports
 * a required update; observed at the root so the blocking
 * forced-update modal appears the moment it becomes `true`.
 */
val SharedStates.isForcedUpdateRequired: StateStream<Boolean>
    get() = state("isForcedUpdateRequired") { false }
