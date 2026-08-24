//
//  AppConstants+ChatInfoPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object ChatInfoPageViewFloats {
    val avatarGlyphSize: Dp = 52.dp
    val avatarSize: Dp = 100.dp
    val avatarTopPadding: Dp = 20.dp
    val bottomSpacerPadding: Dp = 24.dp
    val cardCornerRadius: Dp = 16.dp
    val cardHorizontalMargin: Dp = 16.dp
    val cardHorizontalPadding: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    val cardVerticalMargin: Dp = 8.dp
    val changeNameBottomPadding: Dp = 5.dp
    val changeNameTopPadding: Dp = 12.dp
    val chevronBoxBorderWidth: Dp = 1.dp
    val chevronBoxSize: Dp = 24.dp
    val chevronBoxStartPadding: Dp = 8.dp
    val chevronGlyphSize: Dp = 16.dp
    val doneButtonGlyphSize: Dp = 20.dp
    val headerHorizontalPadding: Dp = 16.dp
    val headerTopPadding: Dp = 12.dp
    val languageBadgeCornerRadius: Dp = 4.dp
    val languageBadgeHorizontalPadding: Dp = 6.dp
    val languageBadgeStartPadding: Dp = 8.dp
    val languageBadgeVerticalPadding: Dp = 2.dp
    val rowAvatarGlyphSize: Dp = 22.dp
    val rowAvatarSize: Dp = 40.dp
    val rowTextStartPadding: Dp = 12.dp
    val rowVerticalPadding: Dp = 10.dp
    val subtitleTopPadding: Dp = 2.dp
    val titleHorizontalPadding: Dp = 24.dp
    val titleTopPadding: Dp = 12.dp
}

// MARK: - Color

object ChatInfoPageViewColors {
    val destructive = Color(0xFFFF3B30)
}

// MARK: - String

object ChatInfoPageViewStrings {
    const val ADD_CONTACT = "Add Contact"
    const val BLOCK = "Block"
    const val CHANGE_NAME_AND_PHOTO = "Change name and photo"
    const val DELETE_CONVERSATION = "Delete this Conversation"
    const val DONE = "Done"
    const val LEAVE_CONVERSATION = "Leave this Conversation"
    const val PARTICIPANTS_SEPARATOR = ", "
    const val REPORT = "Report"
    const val TITLE_ADDITIONAL_SEPARATOR = " + "
    const val UNKNOWN = "Unknown"
}
