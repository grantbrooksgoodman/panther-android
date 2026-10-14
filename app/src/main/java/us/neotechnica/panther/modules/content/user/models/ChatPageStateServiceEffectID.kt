//
//  ChatPageStateServiceEffectID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.models

/**
 * A unique identifier for an effect registered with
 * [ChatPageStateService][us.neotechnica.panther.modules.content.user.services.ChatPageStateService].
 *
 * @property rawValue The string that identifies the effect.
 */
data class ChatPageStateServiceEffectID(
    val rawValue: String,
) {
    companion object {
        /** The effect that opens the chat a notification targets once the current chat dismisses. */
        val deeplinkToOtherChat = ChatPageStateServiceEffectID("deeplinkToOtherChat")

        /** The effect that marks the current conversation stale once the chat page dismisses. */
        val markConversationStale = ChatPageStateServiceEffectID("markConversationStale")

        /** The effect that refreshes the conversations page once the chat page dismisses. */
        val updateAppearance = ChatPageStateServiceEffectID("updateAppearance")
    }
}
