//
//  ContactSelectorPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the contact selector page. */
object ContactSelectorPageViewStrings : TranslatedLabelStrings {
    /** The page's navigation title. */
    val navigationTitle = TranslatedLabelStringCollection("contactSelectorPageView.navigationTitle")

    /** The text of the no results label. */
    val noResultsLabelText = TranslatedLabelStringCollection("contactSelectorPageView.noResultsLabelText")

    /** The search bar's placeholder text. */
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
