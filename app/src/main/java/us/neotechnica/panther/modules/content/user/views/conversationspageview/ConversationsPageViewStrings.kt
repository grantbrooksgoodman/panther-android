//
//  ConversationsPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the conversations page. */
object ConversationsPageViewStrings : TranslatedLabelStrings {
    val navigationTitle = TranslatedLabelStringCollection("conversationsPageView.navigationTitle")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(navigationTitle, TranslationInput("Messages")),
        )
}
