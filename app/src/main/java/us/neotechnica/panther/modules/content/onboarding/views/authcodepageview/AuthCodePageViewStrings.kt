//
//  AuthCodePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.authcodepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the verification-code page. */
object AuthCodePageViewStrings : TranslatedLabelStrings {
    /** The text of the back button. */
    val backButtonText = TranslatedLabelStringCollection("authCodePageView.backButtonText")

    /** The text of the continue button. */
    val continueButtonText = TranslatedLabelStringCollection("authCodePageView.continueButtonText")

    /** The text of the instruction label. */
    val instructionLabelText = TranslatedLabelStringCollection("authCodePageView.instructionLabelText")

    /** The text of the instruction view subtitle label. */
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("authCodePageView.instructionViewSubtitleLabelText")

    /** The text of the instruction view title label. */
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
