//
//  SelfWriteRegistry.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.services

import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.networking.modules.session.models.SelfWriteRecord
import us.neotechnica.panther.subsystem.modules.foundation.models.LockIsolated

/**
 * A registry that tracks recent local writes to conversations, so
 * remote observers can ignore the app's own writes.
 */
object SelfWriteRegistry {
    // MARK: - Properties

    private val records = LockIsolated(setOf<SelfWriteRecord>())

    // MARK: - Methods

    /**
     * Returns whether a recent, unexpired write is recorded for the
     * conversation with the given identifier.
     *
     * @param conversationID The identifier of the conversation to check.
     */
    fun contains(conversationID: ConversationID): Boolean =
        records.wrappedValue.any {
            it.conversationID == conversationID && !it.isExpired
        }

    /**
     * Records a local write for the given conversation, pruning any
     * expired records.
     *
     * @param conversationID The identifier of the conversation that was
     *   written.
     */
    fun record(conversationID: ConversationID) {
        records.withValue { reference ->
            reference.value =
                reference.value
                    .filter { !it.isExpired }
                    .toSet() + SelfWriteRecord(conversationID)
        }
    }
}
