//
//  ConversationsPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 16/01/2024.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the conversations page. */
object ConversationsPageViewStrings : TranslatedLabelStrings {
    val navigationTitle = TranslatedLabelStringCollection("conversationsPageView.navigationTitle")
    val noConversationsLabelText = TranslatedLabelStringCollection("conversationsPageView.noConversationsLabelText")
    val searchBarPlaceholder = TranslatedLabelStringCollection("conversationsPageView.searchBarPlaceholder")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(navigationTitle, TranslationInput("Messages")),
            TranslationInputMap(noConversationsLabelText, TranslationInput("No conversations yet.")),
            TranslationInputMap(searchBarPlaceholder, TranslationInput("Search")),
        )
}
