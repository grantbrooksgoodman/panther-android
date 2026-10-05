//
//  SharedEvents.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.modules.session.state.models.SessionStoreChange
import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/** An event that signals the chat info page entered its loading state. */
val SharedEvents.chatInfoPageLoadingStateUpdated: EventStream<Unit>
    get() = event("chatInfoPageLoadingStateUpdated")

/** An event that signals the conversations page reappeared. */
val SharedEvents.conversationsPageReappeared: EventStream<Unit>
    get() = event("conversationsPageReappeared")

/** An event that signals the current conversation's activities changed. */
val SharedEvents.currentConversationActivityChanged: EventStream<Unit>
    get() = event("currentConversationActivityChanged")

/** An event that signals the current conversation's metadata changed. */
val SharedEvents.currentConversationMetadataChanged: EventStream<Unit>
    get() = event("currentConversationMetadataChanged")

/**
 * An event that fires whenever the message outbox changes – an entry
 * is enqueued, claimed for retry, marked failed, or removed.
 */
val SharedEvents.messageOutboxDidChange: EventStream<Unit>
    get() = event("messageOutboxDidChange")

/** An event that signals network activity occurred. */
val SharedEvents.networkActivityOccurred: EventStream<Unit>
    get() = event("networkActivityOccurred")

/** An event that requests dismissal of the keyboard, resigning any active first responder. */
val SharedEvents.resignFirstResponders: EventStream<Unit>
    get() = event("resignFirstResponders")

/**
 * An event that fires whenever the session store changes, carrying a
 * [SessionStoreChange] describing what was upserted or removed.
 */
val SharedEvents.sessionStoreDidChange: EventStream<SessionStoreChange>
    get() = event("sessionStoreDidChange")

/** An event that signals the trait collection changed. */
val SharedEvents.traitCollectionChanged: EventStream<Unit>
    get() = event("traitCollectionChanged")

/** An event that signals the contact pair archive was updated. */
val SharedEvents.updatedContactPairArchive: EventStream<Unit>
    get() = event("updatedContactPairArchive")
