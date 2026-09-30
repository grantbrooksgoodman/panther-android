//
//  InviteQRCodePageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 30/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.inviteqrcodepageview

import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.translator.models.TranslationInput

/** The translated label strings for the invite QR code page. */
object InviteQRCodePageViewStrings : TranslatedLabelStrings {
    val instructionLabelText = TranslatedLabelStringCollection("inviteQRCodePageView.instructionLabelText")

    override val keyPairs: List<TranslationInputMap> =
        listOf(
            TranslationInputMap(instructionLabelText, TranslationInput("Scan to download ⌘Hello⌘")),
        )
}
