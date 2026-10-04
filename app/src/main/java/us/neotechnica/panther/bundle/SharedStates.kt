//
//  SharedStates.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.StateStream

/** The current query in the conversations page search field. */
val SharedStates.conversationsSearchQuery: StateStream<String>
    get() = state("conversationsSearchQuery") { "" }

/** Whether the new chat page's done toolbar button is enabled. */
val SharedStates.isNewChatPageDoneToolbarButtonEnabled: StateStream<Boolean>
    get() = state("isNewChatPageDoneToolbarButtonEnabled") { true }

/** The set of conversation ID keys currently reloading. */
val SharedStates.reloadingConversationIDKeys: StateStream<Set<String>>
    get() = state("reloadingConversationIDKeys") { emptySet() }
