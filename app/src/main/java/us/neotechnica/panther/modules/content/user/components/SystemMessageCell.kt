//
//  SystemMessageCell.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.modules.content.user.constants.SystemMessageCellColors
import us.neotechnica.panther.modules.content.user.constants.SystemMessageCellFloats
import us.neotechnica.panther.modules.content.user.extensions.attributedSystemString
import us.neotechnica.panther.modules.networking.sources.message.models.Message

// MARK: - Constants Accessors

private typealias SystemCellFloats = SystemMessageCellFloats

/**
 * A row that displays a system message: centered, bubble-less text
 * combining the message's date separator and activity text, limited to
 * three lines and truncated at the tail.
 *
 * @param message The system message to display.
 */
@Composable
fun SystemMessageCell(message: Message) {
    val attributedString = message.attributedSystemString ?: return
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val lineHeight = SystemCellFloats.ACTIVITY_STRING_SYSTEM_FONT_SIZE + SystemCellFloats.labelParagraphStyleLineSpacing.value

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SystemCellFloats.defaultHeight)
                .padding(vertical = SystemCellFloats.additionalVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = attributedString,
            color = SystemMessageCellColors.activityStringForeground(isDarkModeActive),
            style =
                TextStyle(
                    fontSize = (SystemCellFloats.ACTIVITY_STRING_SYSTEM_FONT_SIZE * SystemCellFloats.LABEL_MINIMUM_SCALE_FACTOR).sp,
                    lineHeight = lineHeight.sp,
                ),
            maxLines = SystemCellFloats.LABEL_NUMBER_OF_LINES,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
