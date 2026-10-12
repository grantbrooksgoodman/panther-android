//
//  AppConstants+UserContentExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object UserContentExtensionsFloats {
    object AudioItem {
        val sizeHeight: Dp = 40.dp
        val sizeWidth: Dp = 160.dp
    }

    object Date {
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_BOLD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_STANDARD_ATTRIBUTES_SYSTEM_FONT_SIZE = 12f

        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_UNDER_YEAR_PRIMARY_COMPARATOR = -604_800L
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_UNDER_YEAR_SECONDARY_COMPARATOR = -31_540_000L
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_WEEKDAY_COMPARATOR = -604_800L
        const val CHAT_PAGE_MESSAGE_SEPARATOR_ATTRIBUTED_DATE_STRING_YESTERDAY_COMPARATOR = -86_400L
    }

    object AnnotatedString {
        const val MESSAGE_CELL_STRING_PARAGRAPH_LINE_SPACING = 1.25f
        const val MESSAGE_CELL_STRING_SYSTEM_FONT_SIZE = 18f
    }
}

// MARK: - Color

object UserContentExtensionsColors {
    object Date {
        val chatPageMessageSeparatorAttributedDateStringBoldAttributesDarkForeground = Color.LightGray
        val chatPageMessageSeparatorAttributedDateStringBoldAttributesLightForeground = Color(0xFF8E8E93)
        val chatPageMessageSeparatorAttributedDateStringStandardAttributesDarkForeground = Color.LightGray
        val chatPageMessageSeparatorAttributedDateStringStandardAttributesLightForeground = Color(0xFF8E8E93)
    }

    object Message {
        val kindAttributedTextCurrentUserForeground = Color.White
        val kindAttributedTextDarkForeground = Color.White
        val kindAttributedTextLightForeground = Color.Black
    }
}

// MARK: - String

object UserContentExtensionsStrings {
    object Date {
        const val OVER_YEAR_EN_US_DATE_FORMAT = "MMM dd yyyy,"
        const val OVER_YEAR_DATE_FORMAT = "dd MMM yyyy,"
        const val UNDER_YEAR_EN_US_DATE_FORMAT = "E MMM d,"
        const val UNDER_YEAR_DATE_FORMAT = "E d MMM,"
        const val EN_US_LANGUAGE_ID = "en-US"
        const val SEPARATOR = ","
    }

    object UIImage {
        const val DOCUMENT_IMAGE_SYSTEM_NAME = "doc.circle.fill"
        const val MISSING_IMAGE_SYSTEM_NAME = "questionmark.square.dashed"
    }
}
