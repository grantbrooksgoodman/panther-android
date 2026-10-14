//
//  VerifyNumberPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.verifynumberpageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the phone-number entry page. */
object VerifyNumberPageViewStrings : TranslatedLabelStrings {
    /** The text of the back button. */
    val backButtonText = TranslatedLabelStringCollection("verifyNumberPageView.backButtonText")

    /** The text of the continue button. */
    val continueButtonText = TranslatedLabelStringCollection("verifyNumberPageView.continueButtonText")

    /** The text of the instruction label. */
    val instructionLabelText = TranslatedLabelStringCollection("verifyNumberPageView.instructionLabelText")

    /** The text of the instruction view title label. */
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("verifyNumberPageView.instructionViewTitleLabelText")

    /** The text of the instruction view subtitle label. */
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("verifyNumberPageView.instructionViewSubtitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(continueButtonText, TranslationInput("Continue")),
            TranslationInputMap(instructionLabelText, TranslationInput("Enter your phone number below:")),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Enter Phone Number")),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "Next, enter your phone number.\n\nA verification code will be sent to your number. " +
                        "Standard messaging rates apply.",
                ),
            ),
        )
}
