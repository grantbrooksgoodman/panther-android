//
//  Date+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsFloats
import us.neotechnica.panther.modules.content.user.constants.UserContentExtensionsStrings
import us.neotechnica.panther.subsystem.modules.foundation.extensions.weekdayString
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// MARK: - Constants Accessors

private typealias DateFloats = UserContentExtensionsFloats.Date
private typealias DateStrings = UserContentExtensionsStrings.Date

/**
 * The attributed date string shown as a separator between messages
 * sent on different days, or `null` if it cannot be composed.
 *
 * Formats the date relative to today – as Today, Yesterday, a weekday,
 * or a full date – with the time appended. The day portion renders
 * bold; the caller supplies the color.
 */
@Suppress("VariableMaxLength")
val Date.chatPageMessageSeparatorAttributedDateString: AnnotatedString?
    get() {
        val calendar = Calendar.getInstance()
        val distance = (calendar.startOfDay(this).time - calendar.startOfDay(Date()).time) / MILLISECONDS_PER_SECOND
        val timeString = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(this)
        val isSystemLanguageEnglishUS = Locale.getDefault().toLanguageTag() == DateStrings.EN_US_LANGUAGE_ID

        val languageLocale = Locale.forLanguageTag(RuntimeStorage.languageCode)
        val overYearFormat =
            if (isSystemLanguageEnglishUS) DateStrings.OVER_YEAR_EN_US_DATE_FORMAT else DateStrings.OVER_YEAR_DATE_FORMAT
        val underYearFormat =
            if (isSystemLanguageEnglishUS) DateStrings.UNDER_YEAR_EN_US_DATE_FORMAT else DateStrings.UNDER_YEAR_DATE_FORMAT
        val overYearString = SimpleDateFormat(overYearFormat, languageLocale).format(this)
        val underYearString = SimpleDateFormat(underYearFormat, languageLocale).format(this)

        val overYearStringSeparatorIndex = overYearString.split(DateStrings.SEPARATOR).firstOrNull()?.length ?: return null
        val underYearStringSeparatorIndex = underYearString.split(DateStrings.SEPARATOR).firstOrNull()?.length ?: return null

        if (distance == 0L) {
            val separator = dayWord(LocalizedStringKey.Today)
            return attributedForChatPageMessageSeparator("$separator $timeString", separator.length)
        } else if (distance == DateFloats.CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_YESTERDAY_COMPARATOR) {
            val separator = dayWord(LocalizedStringKey.Yesterday)
            return attributedForChatPageMessageSeparator("$separator $timeString", separator.length)
        } else if (distance >= DateFloats.CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_WEEKDAY_COMPARATOR) {
            val weekdayString = weekdayString()
            val currentWeekdayString = Date().weekdayString()

            if (weekdayString != currentWeekdayString) {
                return attributedForChatPageMessageSeparator("$weekdayString $timeString", weekdayString.length)
            }

            return attributedForChatPageMessageSeparator("$underYearString $timeString", underYearStringSeparatorIndex + 1)
        } else if (distance < DateFloats.CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_UNDER_YEAR_PRIMARY_COMPARATOR &&
            distance > DateFloats.CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_UNDER_YEAR_SECONDARY_COMPARATOR
        ) {
            return attributedForChatPageMessageSeparator("$underYearString $timeString", underYearStringSeparatorIndex + 1)
        }

        return attributedForChatPageMessageSeparator("$overYearString $timeString", overYearStringSeparatorIndex + 1)
    }

// MARK: - Auxiliary

private fun attributedForChatPageMessageSeparator(
    string: String,
    separatorIndex: Int,
): AnnotatedString =
    buildAnnotatedString {
        append(string)
        val boldEnd = separatorIndex.coerceIn(0, string.length)
        addStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                fontSize = DateFloats.CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE.sp,
            ),
            0,
            boldEnd,
        )
        addStyle(
            SpanStyle(fontSize = DateFloats.CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_STANDARD_ATTRIBUTES_SYSTEM_FONT_SIZE.sp),
            boldEnd,
            string.length,
        )
    }

private fun Calendar.startOfDay(date: Date): Date {
    time = date
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return time
}

// Day words live in the app table with a subsystem fallback.
private fun dayWord(key: LocalizedStringKey): String {
    val appValue = key.localized(LocalizationSource.APP)
    return if (appValue != LocalizedStringResolver.MISSING) appValue else key.localized(LocalizationSource.SUBSYSTEM)
}

private const val MILLISECONDS_PER_SECOND = 1000L
