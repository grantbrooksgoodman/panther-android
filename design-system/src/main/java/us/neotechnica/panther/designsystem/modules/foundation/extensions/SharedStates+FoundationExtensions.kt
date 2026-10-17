//
//  SharedStates+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.StateStream

/**
 * A string describing the view the app currently presents at the
 * deepest level of its navigation, or `null` when none is known.
 *
 * Bug reports record this value as the view identifier in their
 * metadata. Update it whenever the app's navigation state changes.
 */
val SharedStates.leafViewDescriptor: StateStream<String?>
    get() = state("leafViewDescriptor") { null }

/**
 * The number of views the app currently presents.
 *
 * The build info overlay reports this value in its statistics
 * label. Update it whenever the app's navigation state changes.
 */
val SharedStates.presentedViewsCount: StateStream<Int>
    get() = state("presentedViewsCount") { 0 }
