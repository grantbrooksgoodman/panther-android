//
//  MessageDeliveryServiceEffectID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
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
        /** The effect that reconfigures the input bar once a send completes. */
        val configureInputBar = MessageDeliveryServiceEffectID("configureInputBar")

        /** The effect that reloads the message list once a send completes. */
        val reloadCollectionView = MessageDeliveryServiceEffectID("reloadCollectionView")

        /** The effect that refreshes the chat info page once a send completes. */
        val updateChatInfoPageView = MessageDeliveryServiceEffectID("updateChatInfoPageView")

        /** The effect that refreshes the conversations page once a send completes. */
        val updateConversations = MessageDeliveryServiceEffectID("updateConversations")

        /** The effect that updates the current user's typing indicator once a send completes. */
        val updateIsTypingForCurrentUser = MessageDeliveryServiceEffectID("updateIsTypingForCurrentUser")
    }
}
