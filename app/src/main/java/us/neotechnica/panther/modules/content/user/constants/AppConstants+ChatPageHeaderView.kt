//
//  AppConstants+ChatPageHeaderView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object ChatPageHeaderViewFloats {
    val avatarGlyphSize: Dp = 22.dp
    val avatarPillOverlap: Dp = 3.dp
    val avatarSize: Dp = 44.dp
    val horizontalPadding: Dp = 12.dp
    val pillChevronSize: Dp = 14.dp
    val pillChevronStartPadding: Dp = 2.dp
    val pillCornerRadius: Dp = 16.dp
    val pillEndPadding: Dp = 8.dp
    val pillStartPadding: Dp = 12.dp
    val pillVerticalPadding: Dp = 4.dp
    val verticalPadding: Dp = 8.dp
}

// MARK: - String

object ChatPageHeaderViewStrings {
    const val AVATAR_FALLBACK_SYMBOL = "person.crop.circle.fill"
    const val BACK_BUTTON_IMAGE_SYSTEM_NAME = "chevron.left"
    const val CHAT_INFO_CHEVRON_IMAGE_SYSTEM_NAME = "chevron.right"
    const val GROUP_AVATAR_FALLBACK_SYMBOL = "person.2"
}
