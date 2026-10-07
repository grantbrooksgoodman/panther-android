//
//  Conversation+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.subsystem.modules.localization.models.localized

/**
 * The text shown in the chat page header, or `null` when the
 * conversation has a name or two or fewer participants.
 *
 * For an unnamed group conversation, this is the number of other
 * participants followed by a localized label.
 */
val Conversation.chatPageHeaderLabelText: String?
    get() {
        if (!metadata.name.isBangQualifiedEmpty || participants.size <= 2) return null
        return "${participants.size - 1} ${LocalizedStringKey.People.localized()}"
    }
