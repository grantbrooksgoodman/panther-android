//
//  ConversationSyncData.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.session.sync.models

import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.message.models.Message
import us.neotechnica.panther.modules.session.entity.extensions.filteringSystemMessages

/**
 * A snapshot of a conversation's in-progress synchronization state.
 */
class ConversationSyncData(
    conversation: Conversation,
    /** The messages resolved for the conversation. */
    val messages: List<Message> = emptyList(),
    /** The latest serialized conversation data fetched from the server. */
    val newData: Map<String, Any?>,
) {
    /** The conversation being synchronized. */
    val conversation: Conversation = conversation.filteringSystemMessages
}
