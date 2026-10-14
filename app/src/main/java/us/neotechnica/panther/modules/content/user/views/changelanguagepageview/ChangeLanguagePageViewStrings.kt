//
//  ChangeLanguagePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.changelanguagepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the change-language page. */
object ChangeLanguagePageViewStrings : TranslatedLabelStrings {
    /** The text of the confirm button. */
    val confirmButtonText = TranslatedLabelStringCollection("changeLanguagePageView.confirmButtonText")

    /** The text of the instruction view subtitle label. */
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("changeLanguagePageView.instructionViewSubtitleLabelText")

    /** The text of the instruction view title label. */
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("changeLanguagePageView.instructionViewTitleLabelText")

    /** The page's navigation title. */
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
