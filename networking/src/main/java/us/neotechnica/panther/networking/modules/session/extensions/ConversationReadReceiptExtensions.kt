//
//  ConversationReadReceiptExtensions.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.session.extensions

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.conversation.models.ConversationID
import us.neotechnica.panther.networking.modules.schema.message.models.Message
import us.neotechnica.panther.networking.modules.schema.message.models.ReadReceipt
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.services.SessionStore
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.dependencies.timestampDateFormatter
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import java.util.Date

/**
 * Writes a read receipt from the current user for each of the given
 * unread [messages], committing the receipts – and, for one-to-one
 * conversations, the conversation's new hash and last-modified date – in
 * a single atomic fan-out, then upserting the results into the store.
 *
 * @throws Exception if the current user is unset or the write fails.
 */
suspend fun Conversation.updateReadDate(messages: List<Message>) {
    if (messages.isEmpty()) return
    val currentUserID =
        User.currentUserID
            ?: throw Exception("Current user ID has not been set.", metadata = ExceptionMetadata(this))

    val now = Date()
    val readReceipt = ReadReceipt(userID = currentUserID, readDate = now)
    val unreadMessages =
        messages.filter { message -> message.readReceipts?.any { it.userID == currentUserID } != true }
    if (unreadMessages.isEmpty()) return

    val database = Networking.config.databaseDelegate
    val formatter = DependencyValues.current.timestampDateFormatter
    val updates = mutableMapOf<String, Any?>()
    val updatedMessages = mutableListOf<Message>()

    for (message in unreadMessages) {
        val updatedReceipts =
            ((message.readReceipts?.filter { it.userID != currentUserID } ?: emptyList()) + readReceipt)
                .distinct()
        updates["messages/${message.id}/readReceipts"] = updatedReceipts.map { it.encoded }
        updatedMessages.add(message.copy(readReceipts = updatedReceipts))
    }

    var updatedConversation: Conversation? = null
    if (participants.size == 2) {
        val conversationPath = "conversations/${id.key}"
        updates["$conversationPath/metadata/lastModified"] = formatter.format(now)

        val withMetadata = copy(metadata = metadata.copyWith(lastModifiedDate = now))
        val newHash = withMetadata.encodedHash
        updates["$conversationPath/hash"] = newHash

        for (participant in participants) {
            updates["users/${participant.userID}/openConversations/${id.key}"] = newHash
        }

        updatedConversation = withMetadata.copy(id = ConversationID(key = id.key, hash = newHash))
    }

    database.commit(updates)
    SessionStore.upsertMessages(updatedMessages.toSet())
    updatedConversation?.let { SessionStore.upsertConversation(it) }
}
