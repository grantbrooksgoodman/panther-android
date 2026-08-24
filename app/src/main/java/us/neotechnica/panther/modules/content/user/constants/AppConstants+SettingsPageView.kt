//
//  AppConstants+SettingsPageView.kt
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

object SettingsPageViewFloats {
    val avatarGlyphSize: Dp = 24.dp
    val avatarSize: Dp = 44.dp
    val cardCornerRadius: Dp = 16.dp
    val cardHorizontalMargin: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    val cardVerticalMargin: Dp = 8.dp
    val contactChevronSize: Dp = 18.dp
    val contactCornerRadius: Dp = 28.dp
    val contactNameStartPadding: Dp = 12.dp
    val doneButtonGlyphSize: Dp = 20.dp
    val headerHorizontalPadding: Dp = 16.dp
    val headerVerticalPadding: Dp = 12.dp
    val iconCornerRadius: Dp = 7.dp
    val iconGlyphSize: Dp = 17.dp
    val iconRowVerticalPadding: Dp = 10.dp
    val iconSize: Dp = 30.dp
    val iconTitleStartPadding: Dp = 12.dp
    val versionBottomPadding: Dp = 24.dp
    val versionTopPadding: Dp = 12.dp

    const val CONTACT_BACKGROUND_ALPHA = 0.55f
}

// MARK: - Color

object SettingsPageViewColors {
    val iconGray = Color(0xFF8E8E93)
    val iconOrange = Color(0xFFFF9500)
    val iconRed = Color(0xFFFF3B30)
}

// MARK: - String

object SettingsPageViewStrings {
    const val DEFAULT_TITLE = "You"
    const val DELETE_ACCOUNT = "Delete account"
    const val DONE = "Done"
    const val HIDE_BUILD_INFO_OVERLAY = "Hide Build Info Overlay"
    const val SHOW_BUILD_INFO_OVERLAY = "Show Build Info Overlay"
    const val SIGN_OUT = "Sign out"
    const val VERSION_PREFIX = "Version "
}
