//
//  ConversationUpdateExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.networking.modules.session.services.SelfWriteRegistry
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash

/**
 * Commits [updated] as a multi-field update of the receiver, writing
 * only [changedKeys] plus the recomputed hash token and every
 * participant's conversation token, in a single atomic fan-out.
 *
 * Mirrors the iOS `Conversation.updateValues`: the fan-out is recorded
 * in the [SelfWriteRegistry] so the app's own write is not echoed back,
 * and the rehashed conversation is upserted into the store.
 *
 * @param updated The conversation as it should become.
 * @param changedKeys The wire keys whose values changed (e.g.
 *   `"activities"`, `"metadata"`, `"participants"`).
 *
 * @return The rehashed, committed conversation.
 */
suspend fun Conversation.commitFieldUpdates(
    updated: Conversation,
    changedKeys: Set<String>,
): Conversation {
    val rehashed = updated.copy(id = ConversationID(key = updated.id.key, hash = updated.encodedHash))
    val conversationPath = "$PATH_CONVERSATIONS/${rehashed.id.key}"
    val encoded = rehashed.encoded

    val updates = mutableMapOf<String, Any?>()
    for (key in changedKeys) encoded[key]?.let { updates["$conversationPath/$key"] = it }
    updates["$conversationPath/$KEY_HASH"] = rehashed.id.hash
    for (participant in rehashed.participants) {
        updates["$PATH_USERS/${participant.userID}/$KEY_OPEN_CONVERSATIONS/${rehashed.id.key}"] = rehashed.id.hash
    }

    SelfWriteRegistry.record(rehashed.id)
    Networking.config.databaseDelegate.commit(updates)
    SessionStore.upsertConversation(rehashed)
    return rehashed
}

private const val PATH_CONVERSATIONS = "conversations"
private const val PATH_USERS = "users"
private const val KEY_HASH = "hash"
private const val KEY_OPEN_CONVERSATIONS = "openConversations"
