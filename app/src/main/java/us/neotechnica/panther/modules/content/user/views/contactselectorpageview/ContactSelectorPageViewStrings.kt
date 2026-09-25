//
//  ContactSelectorPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the contact selector page. */
object ContactSelectorPageViewStrings : TranslatedLabelStrings {
    val navigationTitle = TranslatedLabelStringCollection("contactSelectorPageView.navigationTitle")
    val noResultsLabelText = TranslatedLabelStringCollection("contactSelectorPageView.noResultsLabelText")
    val searchBarPlaceholderText = TranslatedLabelStringCollection("contactSelectorPageView.searchBarPlaceholderText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(navigationTitle, TranslationInput("Add to Conversation")),
            TranslationInputMap(
                noResultsLabelText,
                TranslationInput("No contacts found.\nTap to search for users with this phone number."),
            ),
            TranslationInputMap(searchBarPlaceholderText, TranslationInput("Search contacts or enter phone number")),
        )
}
