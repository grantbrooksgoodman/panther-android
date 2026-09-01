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
    val segmentCornerRadius: Dp = 7.dp
    val segmentVerticalPadding: Dp = 6.dp
    val segmentedControlCornerRadius: Dp = 9.dp
    val segmentedControlHorizontalPadding: Dp = 16.dp
    val segmentedControlTopPadding: Dp = 12.dp
    val segmentedControlTrackPadding: Dp = 2.dp
    val subtitleTopPadding: Dp = 2.dp
    val titleHorizontalPadding: Dp = 24.dp
    val titleTopPadding: Dp = 12.dp
}

// MARK: - Color

object ChatInfoPageViewColors {
    val destructive = Color(0xFFFF3B30)
    val segmentedControlTrack = Color(0x1F787880)
}

// MARK: - String

// Non-translated per-screen constants. The `ChatInfoPageViewStrings`
// name is reserved for the translated-label-strings object (mirroring
// iOS); these format, separator, and Android-specific labels live here.
object ChatInfoPageViewConstants {
    const val BLOCK = "Block"
    const val DELETE_CONVERSATION = "Delete this Conversation"
    const val DONE = "Done"
    const val FILE_TYPE_SEPARATOR = " • "
    const val PARTICIPANTS_SEPARATOR = ", "
    const val REPORT = "Report"
    const val TIMESTAMP_FORMAT = "MMM d, yyyy"
    const val TITLE_ADDITIONAL_SEPARATOR = " + "
    const val UNKNOWN = "Unknown"
}
