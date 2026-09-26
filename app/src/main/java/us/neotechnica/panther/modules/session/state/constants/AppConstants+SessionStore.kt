//
//  AppConstants+SessionStore.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.state.constants

// MARK: - Float

object SessionStoreFloats {
    /**
     * The maximum number of messages persisted per conversation.
     * In-memory state retains all messages for the session; this cap
     * applies only to the on-disk archive snapshot.
     */
    const val MESSAGE_ARCHIVE_CAP_PER_CONVERSATION = 500
}
