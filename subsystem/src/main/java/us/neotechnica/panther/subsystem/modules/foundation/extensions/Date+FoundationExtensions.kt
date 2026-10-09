//
//  Date+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import java.util.Calendar
import java.util.Date

/**
 * The date at noon on the same calendar day, in the current time
 * zone.
 *
 * Use this value to compare dates by calendar day. Anchoring both
 * dates at noon keeps day arithmetic stable across daylight saving
 * transitions, which shorten or lengthen a day by an hour.
 */
val Date.comparator: Date
    get() =
        Calendar
            .getInstance()
            .apply {
                time = this@comparator
                set(Calendar.HOUR_OF_DAY, NOON_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

private const val NOON_HOUR = 12
