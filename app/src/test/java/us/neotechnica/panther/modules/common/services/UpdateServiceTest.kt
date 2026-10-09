//
//  UpdateServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class UpdateServiceTest {
    // MARK: - Properties

    private val originalTimeZone = TimeZone.getDefault()

    // MARK: - Setup

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    // MARK: - Tests

    @Test
    fun `days passed counts calendar days regardless of the time of day`() {
        assertEquals(0L, UpdateService.daysPassed(date(2026, 1, 10, 0, 5), date(2026, 1, 10, 23, 55)))
        assertEquals(1L, UpdateService.daysPassed(date(2026, 1, 10, 23, 55), date(2026, 1, 11, 0, 5)))
        assertEquals(10L, UpdateService.daysPassed(date(2026, 1, 1, 9, 0), date(2026, 1, 11, 8, 0)))
    }

    @Test
    fun `days passed is unaffected by daylight saving transitions`() {
        // Spring forward: 2026-03-08 is 23 hours long.
        assertEquals(2L, UpdateService.daysPassed(date(2026, 3, 7, 23, 30), date(2026, 3, 9, 0, 30)))

        // Fall back: 2026-11-01 is 25 hours long.
        assertEquals(1L, UpdateService.daysPassed(date(2026, 11, 1, 0, 30), date(2026, 11, 2, 0, 15)))
    }

    @Test
    fun `days passed is negative when the start date is in the future`() {
        assertEquals(-3L, UpdateService.daysPassed(date(2026, 5, 4, 12, 0), date(2026, 5, 1, 12, 0)))
    }

    // MARK: - Auxiliary

    private fun date(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, day, hour, minute)
            }.time
}
