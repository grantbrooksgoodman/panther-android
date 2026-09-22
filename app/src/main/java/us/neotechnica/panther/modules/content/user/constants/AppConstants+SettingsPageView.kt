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
}

// MARK: - Color

object SettingsPageViewColors {
    val iconBlue = Color(0xFF007AFF)
    val iconGray = Color(0xFF8E8E93)
    val iconIndigo = Color(0xFF5856D6)
    val iconMint = Color(0xFF00C7BE)
    val iconOrange = Color(0xFFFF9500)
    val iconPink = Color(0xFFFF2D55)
    val iconRed = Color(0xFFFF3B30)
    val iconYellow = Color(0xFFFFCC00)
}

// MARK: - String

object SettingsPageViewStrings {
    const val BLOCKED_USERS = "Blocked users"
    const val BLOCKED_USERS_EMPTY = "No blocked users."
    const val CHANGE_LANGUAGE = "Change language"
    const val CLEAR_CACHES = "Clear caches"
    const val CLEAR_CACHES_CONFIRM_MESSAGE =
        "Are you sure you'd like to clear all caches?\n\n" +
            "This may fix some issues, but can also temporarily slow down the app while indexes rebuild.\n\n" +
            "You will need to restart the app for this to take effect."
    const val CLEAR_CACHES_DONE_MESSAGE = "Caches have been cleared. You must now restart the app."
    const val DEFAULT_TITLE = "You"
    const val DELETE_ACCOUNT = "Delete account"
    const val FEEDBACK_SUBJECT = "Hello — Feedback"
    const val HIDE_BUILD_INFO_OVERLAY = "Hide Build Info Overlay"
    const val INVITE_FRIENDS = "Invite friends"
    const val INVITE_MESSAGE = "Come chat with me on Hello!"
    const val LEAVE_REVIEW = "Leave review"
    const val SEND_FEEDBACK = "Send feedback"
    const val SHARE_TO_ANOTHER_APP = "Share to Another App"
    const val SHOW_BUILD_INFO_OVERLAY = "Show Build Info Overlay"
    const val SIGN_OUT = "Sign out"
    const val UNBLOCK = "Unblock"
    const val UNKNOWN = "Unknown"
    const val VERSION_PREFIX = "Version "
}
