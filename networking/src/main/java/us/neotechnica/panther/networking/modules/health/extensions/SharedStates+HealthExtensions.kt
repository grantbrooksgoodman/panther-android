//
//  SharedStates+HealthExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.health.extensions

import us.neotechnica.panther.networking.modules.health.models.NetworkHealth
import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.StateStream

/**
 * The most recently published network health value.
 *
 * Observe the `changes` stream to react to changes in network
 * quality. The stream yields the current value immediately upon
 * subscription, then each subsequent change.
 */
val SharedStates.networkHealth: StateStream<NetworkHealth>
    get() = state("networkHealth") { NetworkHealth.Unknown }
