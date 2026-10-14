//
//  WelcomePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the welcome page. */
object WelcomePageViewStrings : TranslatedLabelStrings {
    /** The text of the continue button. */
    val continueButtonText = TranslatedLabelStringCollection("welcomePageView.continueButtonText")

    /** The text of the sign in button. */
    val signInButtonText = TranslatedLabelStringCollection("welcomePageView.signInButtonText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(continueButtonText, TranslationInput("Get Started", alternate = "Create an Account")),
            TranslationInputMap(signInButtonText, TranslationInput("Sign In")),
        )
}
