//
//  MessageDeliveryServiceEffectID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

/**
 * A unique identifier for an effect registered with
 * [MessageDeliveryService][us.neotechnica.panther.modules.content.user.services.MessageDeliveryService].
 *
 * @property rawValue The string that identifies the effect.
 */
data class MessageDeliveryServiceEffectID(
    val rawValue: String,
) {
    companion object {
        val configureInputBar = MessageDeliveryServiceEffectID("configureInputBar")
        val reloadCollectionView = MessageDeliveryServiceEffectID("reloadCollectionView")
        val updateChatInfoPageView = MessageDeliveryServiceEffectID("updateChatInfoPageView")
        val updateConversations = MessageDeliveryServiceEffectID("updateConversations")
        val updateIsTypingForCurrentUser = MessageDeliveryServiceEffectID("updateIsTypingForCurrentUser")
    }
}
