//
//  Double+CommonExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.extensions

import kotlin.math.roundToInt

/**
 * The value as a whole-number percentage string.
 *
 * Use this property to display a fractional value – such as `0.75` –
 * as a whole-number percentage string – in this case, `"75"`. The
 * string does not include a percent sign.
 */
val Double.roundedString: String
    get() = (this * PERCENT_MULTIPLIER).roundToInt().toString()

private const val PERCENT_MULTIPLIER = 100
