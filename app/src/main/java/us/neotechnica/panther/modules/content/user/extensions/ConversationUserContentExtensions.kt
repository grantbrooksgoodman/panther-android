//
//  ConversationUserContentExtensions.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation

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
