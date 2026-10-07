//
//  TranslationInput+AlertKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.extensions

import us.neotechnica.panther.designsystem.modules.alertkit.AlertKit
import us.neotechnica.panther.translator.models.TranslationInput

internal val List<TranslationInput>.nonDefaultUnique: List<TranslationInput>
    get() =
        distinctBy { it.value }
            .filter { it.value != AlertKit.Constants.DEFAULT_ACTION_TITLE }
