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

/**
 * An event that fires whenever the message outbox changes – an entry
 * is enqueued, claimed for retry, marked failed, or removed.
 */
val SharedEvents.messageOutboxDidChange: EventStream<Unit>
    get() = event("messageOutboxDidChange")

/**
 * An event that fires when the current conversation is removed from the
 * store (for example, deleted remotely), so open chat UI can dismiss.
 */
val SharedEvents.currentConversationDidBecomeUnavailable: EventStream<Unit>
    get() = event("currentConversationDidBecomeUnavailable")
