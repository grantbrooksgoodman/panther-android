//
//  SelectLanguagePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the language-selection page. */
object SelectLanguagePageViewStrings : TranslatedLabelStrings {
    /** The text of the back button. */
    val backButtonText = TranslatedLabelStringCollection("selectLanguagePageView.backButtonText")

    /** The text of the continue button. */
    val continueButtonText = TranslatedLabelStringCollection("selectLanguagePageView.continueButtonText")

    /** The text of the instruction label. */
    val instructionLabelText = TranslatedLabelStringCollection("selectLanguagePageView.instructionLabelText")

    /** The text of the instruction view subtitle label. */
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("selectLanguagePageView.instructionViewSubtitleLabelText")

    /** The text of the instruction view title label. */
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("selectLanguagePageView.instructionViewTitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(continueButtonText, TranslationInput("Continue")),
            TranslationInputMap(instructionLabelText, TranslationInput("I speak:")),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "To begin, select your native language.\n\nThis will be the language you send and " +
                        "receive messages in, as well as that of system dialogues. Your selection can be " +
                        "changed later in Settings.",
                ),
            ),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Select Native Language")),
        )
}
