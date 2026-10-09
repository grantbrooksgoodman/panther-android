//
//  SharedStates+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.common.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.StateStream

/**
 * Whether any network operation is currently in flight.
 *
 * Observe the `changes` stream to react to changes in network
 * activity. The stream yields the current value immediately upon
 * subscription, then each subsequent change.
 */
val SharedStates.isNetworkActivityOccurring: StateStream<Boolean>
    get() = state("isNetworkActivityOccurring") { false }
