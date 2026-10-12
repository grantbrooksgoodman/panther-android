//
//  Date+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.extensions

import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// MARK: - Types

/** The length of a localized weekday symbol string. */
enum class WeekdaySymbolLength {
    FULL,
    SHORT,
    VERY_SHORT,
}

// MARK: - Properties

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

/**
 * A short display string: a time for today, "Yesterday", a weekday
 * name for the past week, or a short date otherwise.
 *
 * The time renders in the device locale; the short date renders in
 * the app language's locale.
 */
val Date.formattedShortString: String
    get() {
        val currentDate = Date()
        val distanceSeconds = (time - currentDate.time) / MILLISECONDS_PER_SECOND

        if (isInSameDay(currentDate)) {
            return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(this)
        } else if (isInSameDay(currentDate.addingDays(-1))) {
            return Localized(SubsystemStringKey.YESTERDAY).wrappedValue
        } else if (isInSameWeekOfYear(currentDate) || distanceSeconds >= -SECONDS_PER_WEEK) {
            return weekdayString()
        }

        return DateFormat.getDateInstance(DateFormat.SHORT, languageLocale).format(this)
    }

// MARK: - Methods

/**
 * Returns the number of milliseconds between this date and the given
 * date.
 *
 * @param from The date to measure from.
 *
 * @return The elapsed milliseconds, negative when this date precedes
 *   the given one.
 */
fun Date.milliseconds(from: Date): Long = time - from.time

/**
 * Returns the number of whole seconds between this date and the given
 * date.
 *
 * @param from The date to measure from.
 *
 * @return The elapsed seconds, negative when this date precedes the
 *   given one.
 */
fun Date.seconds(from: Date): Long = milliseconds(from) / MILLISECONDS_PER_SECOND

/**
 * Returns the localized weekday name for this date.
 *
 * @param length The symbol length to render.
 * @param standalone Whether to render the standalone form, used
 *   outside a date context.
 *
 * @return The weekday name in the app language's locale.
 */
fun Date.weekdayString(
    length: WeekdaySymbolLength = WeekdaySymbolLength.FULL,
    standalone: Boolean = false,
): String {
    val letter = if (standalone) STANDALONE_WEEKDAY_PATTERN_LETTER else WEEKDAY_PATTERN_LETTER
    val count =
        when (length) {
            WeekdaySymbolLength.FULL -> FULL_WEEKDAY_PATTERN_LENGTH
            WeekdaySymbolLength.SHORT -> SHORT_WEEKDAY_PATTERN_LENGTH
            WeekdaySymbolLength.VERY_SHORT -> VERY_SHORT_WEEKDAY_PATTERN_LENGTH
        }

    return SimpleDateFormat(letter.toString().repeat(count), languageLocale).format(this)
}

// MARK: - Auxiliary

private val languageLocale: Locale
    get() {
        val languageCode = RuntimeStorage.languageCode
        return if (languageCode == "en") Locale.getDefault() else Locale.forLanguageTag(languageCode)
    }

private fun Date.addingDays(days: Int): Date =
    Calendar
        .getInstance()
        .apply {
            time = this@addingDays
            add(Calendar.DAY_OF_YEAR, days)
        }.time

private fun Date.isInSameDay(other: Date): Boolean = isInSame(other, Calendar.DAY_OF_YEAR)

private fun Date.isInSameWeekOfYear(other: Date): Boolean = isInSame(other, Calendar.WEEK_OF_YEAR)

private fun Date.isInSame(
    other: Date,
    field: Int,
): Boolean {
    val calendar = Calendar.getInstance().apply { time = this@isInSame }
    val otherCalendar = Calendar.getInstance().apply { time = other }
    return calendar.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR) &&
        calendar.get(field) == otherCalendar.get(field)
}

private const val FULL_WEEKDAY_PATTERN_LENGTH = 4
private const val MILLISECONDS_PER_SECOND = 1000L
private const val NOON_HOUR = 12
private const val SECONDS_PER_WEEK = 604_800L
private const val SHORT_WEEKDAY_PATTERN_LENGTH = 3
private const val STANDALONE_WEEKDAY_PATTERN_LETTER = 'c'
private const val VERY_SHORT_WEEKDAY_PATTERN_LENGTH = 5
private const val WEEKDAY_PATTERN_LETTER = 'E'
