//
//  SharedStates.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle

import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.StateStream

/** The current query in the conversations page search field. */
val SharedStates.conversationsSearchQuery: StateStream<String>
    get() = state("conversationsSearchQuery") { "" }

/** The set of conversation ID keys currently reloading. */
val SharedStates.reloadingConversationIDKeys: StateStream<Set<String>>
    get() = state("reloadingConversationIDKeys") { emptySet() }
