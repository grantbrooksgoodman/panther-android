//
//  DurationStringTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.extensions.durationString

/** Exercises the duration string's three formats and its rounding. */
class DurationStringTest {
    @Test
    fun `durations under a minute round up into the seconds slot`() {
        assertEquals("0:00", 0f.durationString)
        assertEquals("0:01", 0.2f.durationString)
        assertEquals("0:09", 8.5f.durationString)
        assertEquals("0:59", 59f.durationString)
    }

    @Test
    fun `durations under an hour render zero-padded minutes and seconds`() {
        assertEquals("01:00", 60f.durationString)
        assertEquals("02:05", 125.9f.durationString)
        assertEquals("59:59", 3599f.durationString)
    }

    @Test
    fun `durations of an hour or more render hours minutes and seconds`() {
        assertEquals("01:00:00", 3600f.durationString)
        assertEquals("01:01:05", 3665f.durationString)
        assertEquals("10:00:30", 36030f.durationString)
    }
}
