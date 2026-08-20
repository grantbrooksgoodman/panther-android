//
//  SharedEventsSessionExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.modules.session.models.SessionStoreChange
import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/**
 * An event that fires whenever the session store changes, carrying a
 * [SessionStoreChange] describing what was upserted or removed.
 */
val SharedEvents.sessionStoreDidChange: EventStream<SessionStoreChange>
    get() = event("sessionStoreDidChange")
