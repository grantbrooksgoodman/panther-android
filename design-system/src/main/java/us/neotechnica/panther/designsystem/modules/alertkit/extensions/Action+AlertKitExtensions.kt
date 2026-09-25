//
//  Action+AlertKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 24/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.extensions

import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.translator.models.Translation

/**
 * Returns copies of the actions with their titles replaced by the
 * matching translation output.
 */
internal fun List<Action>.applying(translations: List<Translation>): List<Action> =
    map {
        Action(
            title = translations.firstOutput(it.title),
            isEnabled = it.isEnabled,
            style = it.style,
            effect = it.effect,
        )
    }
