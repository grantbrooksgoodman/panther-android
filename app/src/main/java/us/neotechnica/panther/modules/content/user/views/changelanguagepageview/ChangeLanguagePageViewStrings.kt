//
//  ChangeLanguagePageViewStrings.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 22/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.changelanguagepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the change-language page. */
object ChangeLanguagePageViewStrings : TranslatedLabelStrings {
    val confirmButtonText = TranslatedLabelStringCollection("changeLanguagePageView.confirmButtonText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("changeLanguagePageView.instructionViewSubtitleLabelText")
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("changeLanguagePageView.instructionViewTitleLabelText")
    val navigationTitle = TranslatedLabelStringCollection("changeLanguagePageView.navigationTitle")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(confirmButtonText, TranslationInput("Confirm")),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "This will change the language in which you send and receive messages, as well as the " +
                        "language of system dialogues.\n\nMessages you have already received in your current " +
                        "language will not be re-translated retroactively. The app must be restarted for this " +
                        "to take effect.",
                ),
            ),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Select Language")),
            TranslationInputMap(navigationTitle, TranslationInput("Change Language")),
        )
}
