//
//  SharedEvents.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

/** An event that signals the conversations page reappeared. */
val SharedEvents.conversationsPageReappeared: EventStream<Unit>
    get() = event("conversationsPageReappeared")

/** An event that signals the current conversation's metadata changed. */
val SharedEvents.currentConversationMetadataChanged: EventStream<Unit>
    get() = event("currentConversationMetadataChanged")

/** An event that signals the first message was sent in a new chat. */
val SharedEvents.firstMessageSentInNewChat: EventStream<Unit>
    get() = event("firstMessageSentInNewChat")

/** An event that signals network activity occurred. */
val SharedEvents.networkActivityOccurred: EventStream<Unit>
    get() = event("networkActivityOccurred")

/** An event that signals the trait collection changed. */
val SharedEvents.traitCollectionChanged: EventStream<Unit>
    get() = event("traitCollectionChanged")

/** An event that signals the contact pair archive was updated. */
val SharedEvents.updatedContactPairArchive: EventStream<Unit>
    get() = event("updatedContactPairArchive")
