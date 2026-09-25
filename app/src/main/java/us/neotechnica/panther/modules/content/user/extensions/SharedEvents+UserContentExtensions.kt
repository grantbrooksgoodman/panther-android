//
//  SharedEvents+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.subsystem.modules.shared.models.EventStream
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents

// MARK: - Chat Info

/** An event that signals the chat info page entered its loading state. */
val SharedEvents.chatInfoPageLoadingStateUpdated: EventStream<Unit>
    get() = event("chatInfoPageLoadingStateUpdated")

/** An event that signals the current conversation's activities changed. */
val SharedEvents.currentConversationActivityChanged: EventStream<Unit>
    get() = event("currentConversationActivityChanged")
