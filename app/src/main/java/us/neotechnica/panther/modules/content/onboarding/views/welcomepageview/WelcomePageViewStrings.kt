//
//  WelcomePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.welcomepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the welcome page. */
object WelcomePageViewStrings : TranslatedLabelStrings {
    val continueButtonText = TranslatedLabelStringCollection("welcomePageView.continueButtonText")
    val signInButtonText = TranslatedLabelStringCollection("welcomePageView.signInButtonText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(continueButtonText, TranslationInput("Get Started", alternate = "Create an Account")),
            TranslationInputMap(signInButtonText, TranslationInput("Sign In")),
        )
}
