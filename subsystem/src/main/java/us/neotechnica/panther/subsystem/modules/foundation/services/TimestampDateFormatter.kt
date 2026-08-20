//
//  TimestampDateFormatter.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.services

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats and parses wire-format timestamps.
 *
 * All serialized dates share one format –
 * `yyyy-MM-dd HH:mm:ss zzz` in the POSIX locale, pinned to UTC –
 * matching the iOS `TimestampDateFormatterDependency`. Formatted
 * output always renders the zone token as `GMT`:
 *
 * ```
 * 2026-08-19 21:30:00 GMT
 * ```
 *
 * Parsing accepts any zone the platform recognizes (`GMT`,
 * `UTC`, `GMT+2`, named zones) and rejects strings without a
 * zone token, mirroring the iOS formatter's tolerance. The
 * golden fixtures in
 * `src/test/resources/parity/timestamp_vectors.json` and
 * `timestamp_parse_vectors.json` pin both behaviors.
 *
 * All methods are thread-safe.
 */
class TimestampDateFormatter {
    // MARK: - Properties

    // Formatting writes the zone as a literal: iOS always emits
    // "GMT" for the pinned UTC zone, and rendering zzz through
    // the platform would emit "UTC" on some JVM versions.
    private val formatFormatter =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss 'GMT'",
            Locale.US,
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val parseFormatter =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss zzz",
            Locale.US,
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

    // MARK: - Methods

    /**
     * Returns the wire-format string for the given date.
     *
     * @param date The date to format.
     *
     * @return The formatted timestamp, rendered in UTC with a
     *   `GMT` zone token.
     */
    fun format(date: Date): String =
        synchronized(formatFormatter) {
            formatFormatter.format(date)
        }

    /**
     * Returns the date represented by the given wire-format
     * string, or `null` if the string cannot be parsed.
     *
     * The entire string must match the wire format, including a
     * zone token.
     *
     * @param string The timestamp string to parse.
     *
     * @return The parsed date, or `null`.
     */
    fun parse(string: String): Date? = parseExact(string) ?: parseCompactOffset(string)

    // MARK: - Auxiliary

    // Foundation accepts compact GMT offsets ("GMT+2") that
    // SimpleDateFormat rejects; normalize and retry.
    private fun parseCompactOffset(string: String): Date? {
        val match = COMPACT_OFFSET_REGEX.matchEntire(string) ?: return null
        val (dateTime, offsetToken) = match.destructured

        val offsetFormatter =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US,
            ).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone(offsetToken)
            }

        val position = ParsePosition(0)
        val date =
            offsetFormatter.parse(
                dateTime,
                position,
            )

        return date.takeIf { position.index == dateTime.length }
    }

    private fun parseExact(string: String): Date? =
        synchronized(parseFormatter) {
            val position = ParsePosition(0)
            val date =
                parseFormatter.parse(
                    string,
                    position,
                )

            date.takeIf { position.index == string.length }
        }

    private companion object {
        val COMPACT_OFFSET_REGEX =
            Regex(
                "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) (GMT[+-]\\d{1,2}(?::\\d{2})?)$",
            )
    }
}
