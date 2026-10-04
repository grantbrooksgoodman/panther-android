//
//  Translation+AlertKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.extensions

import us.neotechnica.panther.translator.models.Translation

/**
 * Returns the output of the first translation whose input matches
 * [inputString], or [inputString] itself when none matches, with
 * AlertKit's emphasis sentinels removed.
 */
internal fun List<Translation>.firstOutput(inputString: String): String =
    (firstOrNull { it.input.value == inputString }?.output ?: inputString).sanitized
