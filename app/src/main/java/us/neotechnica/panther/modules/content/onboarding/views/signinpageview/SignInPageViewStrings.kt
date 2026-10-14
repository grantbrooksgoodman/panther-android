//
//  SignInPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.signinpageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the sign-in page. */
object SignInPageViewStrings : TranslatedLabelStrings {
    /** The text of the back button. */
    val backButtonText = TranslatedLabelStringCollection("signInPageView.backButtonText")

    /** The text of the phone number continue button. */
    val phoneNumberContinueButtonText = TranslatedLabelStringCollection("signInPageView.phoneNumberContinueButtonText")

    /** The text of the verification code continue button. */
    val verificationCodeContinueButtonText =
        TranslatedLabelStringCollection("signInPageView.verificationCodeContinueButtonText")

    /** The text of the phone number instruction label. */
    val phoneNumberInstructionLabelText =
        TranslatedLabelStringCollection("signInPageView.phoneNumberInstructionLabelText")

    /** The text of the verification code instruction label. */
    val verificationCodeInstructionLabelText =
        TranslatedLabelStringCollection("signInPageView.verificationCodeInstructionLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(phoneNumberContinueButtonText, TranslationInput("Continue")),
            TranslationInputMap(verificationCodeContinueButtonText, TranslationInput("Finish")),
            TranslationInputMap(phoneNumberInstructionLabelText, TranslationInput("Enter your phone number below:")),
            TranslationInputMap(
                verificationCodeInstructionLabelText,
                TranslationInput("Enter the code sent to your device:"),
            ),
        )
}
