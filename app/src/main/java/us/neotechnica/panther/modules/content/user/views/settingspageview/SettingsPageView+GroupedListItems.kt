//
//  SettingsPageView+GroupedListItems.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.SquareIconView
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.SquareIconViewFloats
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration

// MARK: - Constants Accessors

private typealias Colors = SettingsPageViewColors
private typealias GroupedFloats = SettingsPageViewFloats
private typealias GroupedStrings = SettingsPageViewConstants

// MARK: - List Items

/** The list row for the blocked users button. */
@Composable
fun BlockedUsersListItem(
    title: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration =
            symbolConfiguration(
                Colors.blockedUsersButtonImageBackground,
                GroupedStrings.BLOCKED_USERS_BUTTON_IMAGE_SYSTEM_NAME,
            ),
        title = title,
        isEnabled = isEnabled,
        onClick = onClick,
    )
}

/** The list row that navigates to the change language page. */
@Composable
fun ChangeLanguageListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration =
            symbolConfiguration(
                Colors.changeLanguageButtonImageBackground,
                GroupedStrings.CHANGE_LANGUAGE_BUTTON_IMAGE_SYSTEM_NAME,
                GroupedFloats.CHANGE_LANGUAGE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE,
            ),
        title = title,
        showsDisclosure = true,
        onClick = onClick,
    )
}

/** The list row for the clear caches button. */
@Composable
fun ClearCachesListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration =
            symbolConfiguration(
                Colors.clearCachesButtonImageBackground,
                GroupedStrings.CLEAR_CACHES_BUTTON_IMAGE_SYSTEM_NAME,
                GroupedFloats.CLEAR_CACHES_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE,
            ),
        title = title,
        onClick = onClick,
    )
}

/** The list row for the delete account button. */
@Composable
fun DeleteAccountListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration =
            symbolConfiguration(
                Colors.deleteAccountButtonImageBackground,
                GroupedStrings.DELETE_ACCOUNT_BUTTON_IMAGE_SYSTEM_NAME,
            ),
        title = title,
        onClick = onClick,
    )
}

/** The list row for the invite friends button. */
@Composable
fun InviteFriendsListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration =
            symbolConfiguration(
                Colors.inviteFriendsButtonImageBackground,
                GroupedStrings.INVITE_FRIENDS_BUTTON_IMAGE_SYSTEM_NAME,
            ),
        title = title,
        onClick = onClick,
    )
}

/** The list row for the leave review button. */
@Composable
fun LeaveReviewListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration = symbolConfiguration(Colors.leaveReviewButtonImageBackground, GroupedStrings.LEAVE_REVIEW_BUTTON_IMAGE_SYSTEM_NAME),
        title = title,
        onClick = onClick,
    )
}

/** The list row for the send feedback button. */
@Composable
fun SendFeedbackListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration =
            symbolConfiguration(
                Colors.sendFeedbackButtonImageBackground,
                GroupedStrings.SEND_FEEDBACK_BUTTON_IMAGE_SYSTEM_NAME,
            ),
        title = title,
        onClick = onClick,
    )
}

/** The list row for the sign out button. */
@Composable
fun SignOutListItem(
    title: String,
    onClick: () -> Unit,
) {
    SettingsListRow(
        configuration = symbolConfiguration(Colors.signOutButtonImageBackground, GroupedStrings.SIGN_OUT_BUTTON_IMAGE_SYSTEM_NAME),
        title = title,
        onClick = onClick,
    )
}

// MARK: - Row

/** A grouped-list row with a square icon, a title, and an optional disclosure indicator. */
@Composable
fun SettingsListRow(
    configuration: SquareIconViewConfiguration,
    title: String,
    isEnabled: Boolean = true,
    showsDisclosure: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (isEnabled) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = GroupedFloats.cardPadding, vertical = GroupedFloats.iconRowVerticalPadding),
    ) {
        SquareIconView(configuration, Modifier.size(GroupedFloats.iconSize))
        Components.Text(
            title,
            foregroundColor = if (isEnabled) colors.titleText else colors.subtitleText,
            modifier = Modifier.weight(1f).padding(start = GroupedFloats.iconTitleStartPadding),
        )
        if (showsDisclosure) {
            Components.Symbol(
                "chevron.forward",
                foregroundColor = colors.subtitleText,
                modifier = Modifier.size(GroupedFloats.disclosureChevronSize),
            )
        }
    }
}

/** A hairline divider separating grouped-list rows. */
@Composable
fun SettingsRowDivider() {
    val colors = LocalPantherColors.current
    HorizontalDivider(
        color = colors.groupedContentBackground,
        modifier = Modifier.padding(start = GroupedFloats.cardPadding + GroupedFloats.iconSize + GroupedFloats.iconTitleStartPadding),
    )
}

// MARK: - Auxiliary

private fun symbolConfiguration(
    backgroundColor: Color,
    systemName: String,
    framePercentOfTotalSize: Float = SquareIconViewFloats.OVERLAY_FRAME_HEIGHT_MULTIPLIER,
): SquareIconViewConfiguration =
    SquareIconViewConfiguration(
        size = DpSize(SettingsPageViewFloats.iconSize, SettingsPageViewFloats.iconSize),
        backgroundColor = backgroundColor,
        overlay =
            SquareIconViewConfiguration.OverlayConfiguration.Symbol(
                name = systemName,
                framePercentOfTotalSize = framePercentOfTotalSize,
            ),
    )
