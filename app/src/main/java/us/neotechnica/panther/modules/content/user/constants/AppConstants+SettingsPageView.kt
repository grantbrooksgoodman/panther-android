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

/** The layout and timing values of the settings page view. */
object SettingsPageViewFloats {
    /** The bottom padding of the build info button image. */
    val buildInfoButtonImageBottomPadding: Dp = 6.dp

    /** The frame height of the build info button image. */
    val buildInfoButtonImageFrameHeight: Dp = (515f / 20f).dp

    /** The frame width of the build info button image. */
    val buildInfoButtonImageFrameWidth: Dp = (1000f / 20f).dp

    /** The bottom padding of the build info button label. */
    val buildInfoButtonLabelBottomPadding: Dp = 8.dp

    /** The bottom padding of the contact detail view. */
    val contactDetailViewBottomPadding: Dp = 20.dp

    /** The horizontal padding of the contact detail view. */
    val contactDetailViewHorizontalPadding: Dp = 20.dp

    /** The top padding of the contact detail view. */
    val contactDetailViewTopPadding: Dp = 30.dp

    /** The delay, in milliseconds, before navigating to onboarding after signing out. */
    const val SIGN_OUT_NAVIGATION_DELAY_MILLISECONDS = 500L

    /** The bottom padding of the grouped list view. */
    val groupedListViewBottomPadding: Dp = 20.dp

    /** The horizontal padding of the grouped list view. */
    val groupedListViewHorizontalPadding: Dp = 20.dp

    /** The fraction of the change language button's icon its overlay occupies. */
    const val CHANGE_LANGUAGE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE = 0.7f

    /** The fraction of the clear caches button's icon its overlay occupies. */
    const val CLEAR_CACHES_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE = 0.6f

    /** The fraction of the toggle developer mode button's icon its overlay occupies. */
    const val TOGGLE_DEVELOPER_MODE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE = 0.6f
}

// MARK: - Color

/** The colors of the settings page view. */
object SettingsPageViewColors {
    /** The background color of the blocked users button image. */
    val blockedUsersButtonImageBackground = Color(0xFF8E8E93)

    /** The background color of the change language button image. */
    val changeLanguageButtonImageBackground = Color(0xFFFF2D55)

    /** The foreground color of the change language button image. */
    val changeLanguageButtonImageForeground = Color.White

    /** The background color of the clear caches button image. */
    val clearCachesButtonImageBackground = Color(0xFF00C7BE)

    /** The background color of the delete account button image. */
    val deleteAccountButtonImageBackground = Color(0xFFFF9500)

    /** The background color of the invite friends button image. */
    val inviteFriendsButtonImageBackground = Color(0xFF007AFF)

    /** The background color of the leave review button image. */
    val leaveReviewButtonImageBackground = Color(0xFFFFCC00)

    /** The background color of the override language code button image. */
    val overrideLanguageCodeButtonImageBackground = Color(0xFF00C7BE)

    /** The background color of the send feedback button image. */
    val sendFeedbackButtonImageBackground = Color(0xFF5856D6)

    /** The background color of the sign out button image. */
    val signOutButtonImageBackground = Color(0xFFFF3B30)

    /** The background color of the toggle developer mode button image. */
    val toggleDeveloperModeButtonImageBackground = Color(0xFFFFCC00)
}

// MARK: - String

// Non-translated per-screen constants. The `SettingsPageViewStrings`
// name is reserved for the translated-label-strings object.

/** The string constants of the settings page view. */
object SettingsPageViewConstants {
    /** The message shown after the account is deleted. */
    const val ACCOUNT_DELETED_MESSAGE = "Your account has been deleted. You must now restart the app."

    /** The text of the blocked users button. */
    const val BLOCKED_USERS_BUTTON_TEXT = "Blocked users"

    /** The change language row's text. */
    const val CHANGE_LANGUAGE = "Change language"

    /** The clear caches action's title. */
    const val CLEAR_CACHES = "Clear Caches"

    /** The text of the clear caches button. */
    const val CLEAR_CACHES_BUTTON_TEXT = "Clear caches"

    /** The delete account action's title. */
    const val DELETE_ACCOUNT = "Delete Account"

    /** The text of the delete account button. */
    const val DELETE_ACCOUNT_BUTTON_TEXT = "Delete account"

    /** The exit action's title. */
    const val EXIT = "Exit"

    /** The file a report action's title. */
    const val FILE_A_REPORT = "File a Report"

    /** The invite friends action's title. */
    const val INVITE_FRIENDS = "Invite Friends"

    /** The text of the invite friends button. */
    const val INVITE_FRIENDS_BUTTON_TEXT = "Invite friends"

    /** The text of the leave review button. */
    const val LEAVE_REVIEW_BUTTON_TEXT = "Leave a review"

    /** The log out action's title. */
    const val LOG_OUT = "Log out"

    /** The text of the override language code button. */
    const val OVERRIDE_LANGUAGE_CODE_BUTTON_TEXT = "Override Language Code to English"

    /** The rate the app action's title. */
    const val RATE_THE_APP = "Rate the app"

    /** The reload action's title. */
    const val RELOAD = "Reload"

    /** The report bug action's title. */
    const val REPORT_BUG = "Report Bug"

    /** The prefix of the restore language button's text, followed by the language name. */
    const val RESTORE_LANGUAGE_CODE_BUTTON_TEXT_PREFIX = "Restore Language to"

    /** The share to another app action's title. */
    const val SHARE_TO_ANOTHER_APP = "Share to Another App"

    /** The show QR code action's title. */
    const val SHOW_QR_CODE = "Show QR Code"

    /** The text of the sign out button. */
    const val SIGN_OUT_BUTTON_TEXT = "Sign out"

    /** The text of the toggle developer mode button. */
    const val TOGGLE_DEVELOPER_MODE_BUTTON_TEXT = "Toggle Developer Mode"

    /** The system name of the blocked users button image. */
    const val BLOCKED_USERS_BUTTON_IMAGE_SYSTEM_NAME = "flag.fill"

    /** The system name of the clear caches button image. */
    const val CLEAR_CACHES_BUTTON_IMAGE_SYSTEM_NAME = "command"

    /** The system name of the delete account button image. */
    const val DELETE_ACCOUNT_BUTTON_IMAGE_SYSTEM_NAME = "trash.fill"

    /** The system name of the invite friends button image. */
    const val INVITE_FRIENDS_BUTTON_IMAGE_SYSTEM_NAME = "location.fill"

    /** The system name of the leave review button image. */
    const val LEAVE_REVIEW_BUTTON_IMAGE_SYSTEM_NAME = "star.fill"

    /** The system name of the override language code button image. */
    const val OVERRIDE_LANGUAGE_CODE_BUTTON_IMAGE_SYSTEM_NAME = "square.text.square.fill"

    /** The system name of the send feedback button image. */
    const val SEND_FEEDBACK_BUTTON_IMAGE_SYSTEM_NAME = "info"

    /** The system name of the sign out button image. */
    const val SIGN_OUT_BUTTON_IMAGE_SYSTEM_NAME = "hand.raised.fill"

    /** The system name of the toggle developer mode button image. */
    const val TOGGLE_DEVELOPER_MODE_BUTTON_IMAGE_SYSTEM_NAME = "command"
}
