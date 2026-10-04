//
//  AppConstants+SettingsPageView.kt
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
    val disclosureChevronSize: Dp = 24.dp
    val doneButtonGlyphSize: Dp = 20.dp
    val headerHorizontalPadding: Dp = 16.dp
    val headerVerticalPadding: Dp = 12.dp
    val iconRowVerticalPadding: Dp = 10.dp
    val iconSize: Dp = 30.dp
    val iconTitleStartPadding: Dp = 12.dp
    val versionBottomPadding: Dp = 24.dp
    val versionTopPadding: Dp = 12.dp

    const val CHANGE_LANGUAGE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE = 0.7f
    const val CLEAR_CACHES_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE = 0.6f
    const val TOGGLE_DEVELOPER_MODE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE = 0.6f
}

// MARK: - Color

object SettingsPageViewColors {
    val blockedUsersButtonImageBackground = Color(0xFF8E8E93)
    val changeLanguageButtonImageBackground = Color(0xFFFF2D55)
    val clearCachesButtonImageBackground = Color(0xFF00C7BE)
    val deleteAccountButtonImageBackground = Color(0xFFFF9500)
    val inviteFriendsButtonImageBackground = Color(0xFF007AFF)
    val leaveReviewButtonImageBackground = Color(0xFFFFCC00)
    val overrideLanguageCodeButtonImageBackground = Color(0xFF00C7BE)
    val sendFeedbackButtonImageBackground = Color(0xFF5856D6)
    val signOutButtonImageBackground = Color(0xFFFF3B30)
    val toggleDeveloperModeButtonImageBackground = Color(0xFFFFCC00)
}

// MARK: - String

object SettingsPageViewConstants {
    const val ACCOUNT_DELETED_MESSAGE = "Your account has been deleted. You must now restart the app."
    const val BLOCKED_USERS_BUTTON_TEXT = "Blocked users"
    const val CHANGE_LANGUAGE = "Change language"
    const val CLEAR_CACHES = "Clear Caches"
    const val CLEAR_CACHES_BUTTON_TEXT = "Clear caches"
    const val DELETE_ACCOUNT = "Delete Account"
    const val DELETE_ACCOUNT_BUTTON_TEXT = "Delete account"
    const val EXIT = "Exit"
    const val FILE_A_REPORT = "File a Report"
    const val INVITE_FRIENDS = "Invite Friends"
    const val INVITE_FRIENDS_BUTTON_TEXT = "Invite friends"
    const val LEAVE_REVIEW_BUTTON_TEXT = "Leave a review"
    const val LOG_OUT = "Log out"
    const val OVERRIDE_LANGUAGE_CODE_BUTTON_TEXT = "Override Language Code to English"
    const val RATE_THE_APP = "Rate the app"
    const val RELOAD = "Reload"
    const val REPORT_BUG = "Report Bug"
    const val RESTORE_LANGUAGE_CODE_BUTTON_TEXT_PREFIX = "Restore Language to"
    const val SHARE_TO_ANOTHER_APP = "Share to Another App"
    const val SHOW_QR_CODE = "Show QR Code"
    const val SIGN_OUT = "Sign out"
    const val SIGN_OUT_BUTTON_TEXT = "Sign out"
    const val TOGGLE_DEVELOPER_MODE = "Toggle Developer Mode"

    const val BLOCKED_USERS_BUTTON_IMAGE_SYSTEM_NAME = "flag.fill"
    const val CHANGE_LANGUAGE_BUTTON_IMAGE_SYSTEM_NAME = "globe"
    const val CLEAR_CACHES_BUTTON_IMAGE_SYSTEM_NAME = "command"
    const val DELETE_ACCOUNT_BUTTON_IMAGE_SYSTEM_NAME = "trash.fill"
    const val INVITE_FRIENDS_BUTTON_IMAGE_SYSTEM_NAME = "location.fill"
    const val LEAVE_REVIEW_BUTTON_IMAGE_SYSTEM_NAME = "star.fill"
    const val OVERRIDE_LANGUAGE_CODE_BUTTON_IMAGE_SYSTEM_NAME = "square.text.square.fill"
    const val SEND_FEEDBACK_BUTTON_IMAGE_SYSTEM_NAME = "info"
    const val SIGN_OUT_BUTTON_IMAGE_SYSTEM_NAME = "hand.raised.fill"
    const val TOGGLE_DEVELOPER_MODE_BUTTON_IMAGE_SYSTEM_NAME = "command"
}
