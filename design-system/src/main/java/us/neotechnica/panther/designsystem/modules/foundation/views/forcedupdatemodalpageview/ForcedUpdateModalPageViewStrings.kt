//
//  ForcedUpdateModalPageViewStrings.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.forcedupdatemodalpageview

import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.translator.models.TranslationInput

/** The translatable strings displayed by the forced-update modal. */
internal enum class ForcedUpdateModalPageViewStringKey {
    INSTALL_BUTTON_TEXT,
    SUBTITLE_LABEL_TEXT,
    TITLE_LABEL_TEXT,
    ;

    // MARK: - Computed Properties

    val alternate: String?
        get() = null

    val rawValue: String
        get() {
            var productName = "⌘${Build.finalName}⌘"
            if (productName.sanitized.isBlank()) {
                productName = if (Build.codeName.isBlank()) "the app" else "⌘${Build.codeName}⌘"
            }

            return when (this) {
                INSTALL_BUTTON_TEXT -> "Install Now"

                SUBTITLE_LABEL_TEXT ->
                    "This version of $productName is no longer supported. " +
                        "To continue, please download and install the most recent update."

                TITLE_LABEL_TEXT -> if (RuntimeStorage.languageCode == "en") "Update Required" else "An Update is Required"
            }
        }
}

internal object ForcedUpdateModalPageViewStrings {
    // MARK: - Computed Properties

    val defaultOutputMap: Map<ForcedUpdateModalPageViewStringKey, String>
        get() = ForcedUpdateModalPageViewStringKey.entries.associateWith { it.rawValue }

    val keyPairs: List<Pair<ForcedUpdateModalPageViewStringKey, TranslationInput>>
        get() =
            ForcedUpdateModalPageViewStringKey.entries.map {
                it to
                    TranslationInput(
                        it.rawValue,
                        alternate = it.alternate,
                    )
            }
}
