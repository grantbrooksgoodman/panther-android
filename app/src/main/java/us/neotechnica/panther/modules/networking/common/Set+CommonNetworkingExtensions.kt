//
//  Set+CommonNetworkingExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.common

import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User

/** A set containing every user data type. */
val allDataTypes: Set<User.DataType>
    get() = User.DataType.entries.toSet()

/**
 * Merges the given conversations into the set, replacing any existing
 * conversations that share a key.
 *
 * @param conversations The conversations to merge in.
 */
fun MutableSet<Conversation>.merge(conversations: Collection<Conversation>) {
    val incomingKeys = conversations.map { it.id.key }
    retainAll { it.id.key !in incomingKeys }
    addAll(conversations)
}
