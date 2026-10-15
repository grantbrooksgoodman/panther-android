//
//  AppConstants+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("VariableMaxLength")

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout values of the user content extensions. */
object UserContentExtensionsFloats {
    /** The values of audio items. */
    object AudioItem {
        /** The height of an audio item. */
        val sizeHeight: Dp = 40.dp

        /** The width of an audio item. */
        val sizeWidth: Dp = 160.dp
    }

    /** The values of date formatting. */
    object Date {
        /** The font size of the bold portion of a message separator's date. */
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        /** The font size of the standard portion of a message separator's date. */
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_STANDARD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        /**
         * The interval, in seconds, beyond which a message separator's date
         * shows the day and month.
         */
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_UNDER_YEAR_PRIMARY_COMPARATOR = -604_800L

        /** The interval, in seconds, within which a message separator's date omits the year. */
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_UNDER_YEAR_SECONDARY_COMPARATOR = -31_540_000L

        /** The interval, in seconds, within which a message separator's date shows the weekday. */
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_WEEKDAY_COMPARATOR = -604_800L

        /** The interval, in seconds, at which a message separator's date reads as yesterday. */
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_YESTERDAY_COMPARATOR = -86_400L
    }

    /** The values of annotated strings. */
    object AnnotatedString {
        /** The line spacing multiplier of a message cell's text. */
        const val MESSAGE_CELL_STRING_PARAGRAPH_LINE_SPACING = 1.25f

        /** The font size of a message cell's text. */
        const val MESSAGE_CELL_STRING_SYSTEM_FONT_SIZE = 18f
    }
}

// MARK: - Color

/** The colors of the user content extensions. */
object UserContentExtensionsColors {
    /** The values of date formatting. */
    object Date {
        /**
         * The foreground color of the standard portion of a message
         * separator's date in dark mode.
         */
        val chatPageMessageSeparatorAttributedDateStringStandardAttributesDarkForeground = Color.LightGray

        /**
         * The foreground color of the standard portion of a message
         * separator's date in light mode.
         */
        val chatPageMessageSeparatorAttributedDateStringStandardAttributesLightForeground = Color(0xFF8E8E93)
    }

    /** The colors of message text. */
    object Message {
        /** The text color of the current user's messages. */
        val kindAttributedTextCurrentUserForeground = Color.White

        /** The text color of received messages in dark mode. */
        val kindAttributedTextDarkForeground = Color.White

        /** The text color of received messages in light mode. */
        val kindAttributedTextLightForeground = Color.Black
    }
}

// MARK: - String

/** The string constants of the user content extensions. */
object UserContentExtensionsStrings {
    /** The values of date formatting. */
    object Date {
        /** The format of a date over a year old in the en-US locale. */
        const val OVER_YEAR_EN_US_DATE_FORMAT = "MMM dd yyyy,"

        /** The format of a date over a year old. */
        const val OVER_YEAR_DATE_FORMAT = "dd MMM yyyy,"

        /** The format of a date under a year old in the en-US locale. */
        const val UNDER_YEAR_EN_US_DATE_FORMAT = "E MMM d,"

        /** The format of a date under a year old. */
        const val UNDER_YEAR_DATE_FORMAT = "E d MMM,"

        /** The en-US language identifier. */
        const val EN_US_LANGUAGE_ID = "en-US"

        /** The separator between the date and the time. */
        const val SEPARATOR = ","
    }

    /** The system names of the images. */
    object UIImage {
        /** The system name of the document image. */
        const val DOCUMENT_IMAGE_SYSTEM_NAME = "doc.circle.fill"

        /** The system name of the missing image. */
        const val MISSING_IMAGE_SYSTEM_NAME = "questionmark.square.dashed"
    }
}
