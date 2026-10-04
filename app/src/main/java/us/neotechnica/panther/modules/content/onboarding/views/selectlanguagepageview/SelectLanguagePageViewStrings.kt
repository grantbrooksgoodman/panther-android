//
//  SelectLanguagePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the language-selection page. */
object SelectLanguagePageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("selectLanguagePageView.backButtonText")
    val continueButtonText = TranslatedLabelStringCollection("selectLanguagePageView.continueButtonText")
    val instructionLabelText = TranslatedLabelStringCollection("selectLanguagePageView.instructionLabelText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("selectLanguagePageView.instructionViewSubtitleLabelText")
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
