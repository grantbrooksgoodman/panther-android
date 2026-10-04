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
    val backButtonText = TranslatedLabelStringCollection("signInPageView.backButtonText")
    val phoneNumberContinueButtonText = TranslatedLabelStringCollection("signInPageView.phoneNumberContinueButtonText")
    val verificationCodeContinueButtonText =
        TranslatedLabelStringCollection("signInPageView.verificationCodeContinueButtonText")
    val phoneNumberInstructionLabelText =
        TranslatedLabelStringCollection("signInPageView.phoneNumberInstructionLabelText")
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
