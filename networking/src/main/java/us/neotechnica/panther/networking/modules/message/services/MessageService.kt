//
//  MessageService.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.message.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata

/**
 * Reads [Message] records from the database, upserting each into the
 * [SessionStore]. Ported from the iOS `MessageService` read path.
 */
object MessageService {
    // MARK: - Properties

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Methods

    /** Returns the messages with the given IDs, upserting them into the store. */
    suspend fun getMessages(ids: List<String>): List<Message> =
        coroutineScope {
            ids
                .map { id -> async { runCatching { getMessage(id) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

    /** Returns the message with the given ID, upserting it into the store. */
    suspend fun getMessage(id: String): Message {
        val data: Map<String, Any?> = database.getValues("${NetworkPath.messages.rawValue}/$id")
        val childData = data.toMutableMap().apply { put(ID_KEY, id) }

        if (!Message.canDecode(childData)) {
            throw Exception(
                "Failed to decode message.",
                userInfo = mapOf("MessageID" to id),
                metadata = ExceptionMetadata(this),
            )
        }

        return Message.decode(childData).also { SessionStore.upsertMessages(setOf(it)) }
    }

    // MARK: - Companion

    private const val ID_KEY = "id"
}
