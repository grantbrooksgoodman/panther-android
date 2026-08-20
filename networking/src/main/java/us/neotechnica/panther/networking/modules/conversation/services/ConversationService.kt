//
//  ConversationService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.conversation.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.BANG_QUALIFIED_EMPTY
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.session.services.SessionStore

/**
 * Reads [Conversation] records from the database, upserting each into
 * the [SessionStore]. Ported from the iOS `ConversationService` read
 * path.
 */
object ConversationService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Methods

    /** Returns the conversations with the given keys, upserting them into the store. */
    suspend fun getConversations(idKeys: List<String>): List<Conversation> =
        coroutineScope {
            idKeys
                .map { idKey -> async { runCatching { getConversation(idKey) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

    /** Returns the conversation with the given key, upserting it into the store. */
    suspend fun getConversation(idKey: String): Conversation {
        val data: Map<String, Any?> = database.getValues("${NetworkPath.conversations.rawValue}/$idKey")
        val hash = data[ENCODED_HASH_KEY] as? String ?: BANG_QUALIFIED_EMPTY
        val childData = data.toMutableMap().apply { put(ID_KEY, "$idKey | $hash") }

        if (!Conversation.canDecode(childData)) {
            throw us.neotechnica.panther.subsystem.modules.foundation.models.Exception(
                "Failed to decode conversation.",
                userInfo = mapOf("ConversationIDKey" to idKey),
                metadata =
                    us.neotechnica.panther.subsystem.modules.foundation.models
                        .ExceptionMetadata(this),
            )
        }

        return Conversation.decode(childData).also { SessionStore.upsertConversation(it) }
    }

    // MARK: - Companion

    private const val ID_KEY = "id"
    private const val ENCODED_HASH_KEY = "hash"
}
