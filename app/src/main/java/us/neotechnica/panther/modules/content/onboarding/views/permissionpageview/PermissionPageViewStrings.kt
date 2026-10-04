//
//  PermissionPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.permissionpageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the permissions page. */
object PermissionPageViewStrings : TranslatedLabelStrings {
    val backButtonText = TranslatedLabelStringCollection("permissionPageView.backButtonText")
    val finishButtonText = TranslatedLabelStringCollection("permissionPageView.finishButtonText")
    val contactPermissionCapsuleButtonText =
        TranslatedLabelStringCollection("permissionPageView.contactPermissionCapsuleButtonText")
    val notificationPermissionCapsuleButtonText =
        TranslatedLabelStringCollection("permissionPageView.notificationPermissionCapsuleButtonText")
    val instructionViewSubtitleLabelText =
        TranslatedLabelStringCollection("permissionPageView.instructionViewSubtitleLabelText")
    val instructionViewTitleLabelText =
        TranslatedLabelStringCollection("permissionPageView.instructionViewTitleLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(backButtonText, TranslationInput("Back", alternate = "Go back")),
            TranslationInputMap(finishButtonText, TranslationInput("Finish")),
            TranslationInputMap(
                contactPermissionCapsuleButtonText,
                TranslationInput("Tap to allow contact access"),
            ),
            TranslationInputMap(
                notificationPermissionCapsuleButtonText,
                TranslationInput("Tap to allow notifications"),
            ),
            TranslationInputMap(
                instructionViewSubtitleLabelText,
                TranslationInput(
                    "Finally, grant Hello the necessary permissions to work with your device.\n\n" +
                        "These options can be changed later in Settings.",
                ),
            ),
            TranslationInputMap(instructionViewTitleLabelText, TranslationInput("Grant Permissions")),
        )
}
