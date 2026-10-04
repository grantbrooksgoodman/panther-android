//
//  AuthCodePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.authcodepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the verification-code page. */
object AuthCodePageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("authCodePageView.backButtonText")
    val continueButtonText = TranslatedLabelStringCollection("authCodePageView.continueButtonText")
    val instructionLabelText = TranslatedLabelStringCollection("authCodePageView.instructionLabelText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("authCodePageView.instructionViewSubtitleLabelText")
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("authCodePageView.instructionViewTitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(continueButtonText, TranslationInput("Continue")),
            TranslationInputMap(instructionLabelText, TranslationInput("Enter the code sent to your device:")),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "A verification code was sent to your device. It may take a minute or so to arrive.",
                ),
            ),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Enter Verification Code")),
        )
}
