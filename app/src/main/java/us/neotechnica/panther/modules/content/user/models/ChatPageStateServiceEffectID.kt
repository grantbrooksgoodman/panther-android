//
//  ChatPageStateServiceEffectID.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
        val deeplinkToOtherChat = ChatPageStateServiceEffectID("deeplinkToOtherChat")
        val markConversationStale = ChatPageStateServiceEffectID("markConversationStale")
        val updateAppearance = ChatPageStateServiceEffectID("updateAppearance")
    }
}
