//
//  String+AlertKitExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import us.neotechnica.panther.designsystem.modules.alertkit.models.AttributedStringConfig

/**
 * The string with AlertKit's emphasis sentinels (`⌘`, `⁂`, `※`)
 * removed.
 */
val String.sanitized: String
    get() =
        replace("⌘", "")
            .replace("⁂", "")
            .replace("※", "")

internal fun String.attributed(config: AttributedStringConfig): AnnotatedString =
    buildAnnotatedString {
        append(this@attributed)
        addStyle(config.primaryAttributes, 0, this@attributed.length)
        config.secondaryAttributes?.forEach { stringAttributes ->
            stringAttributes.stringRanges.forEach { range ->
                val start = this@attributed.indexOf(range)
                if (start >= 0) addStyle(stringAttributes.attributes, start, start + range.length)
            }
        }
    }
