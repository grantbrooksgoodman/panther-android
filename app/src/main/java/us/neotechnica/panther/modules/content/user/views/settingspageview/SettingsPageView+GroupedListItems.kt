//
//  SettingsPageView+GroupedListItems.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.designsystem.modules.foundation.components.GroupedListView
import us.neotechnica.panther.designsystem.modules.foundation.components.ListRowView
import us.neotechnica.panther.modules.content.user.components.SquareIconImage
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.models.SquareIconViewConfiguration
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.localization.models.localized

// MARK: - Constants Accessors

private typealias Colors = SettingsPageViewColors
private typealias GroupedFloats = SettingsPageViewFloats
private typealias GroupedStrings = SettingsPageViewConstants

// MARK: - Grouped List Views

// The page's grouped lists: the app rows, the support rows, the
// account rows, and – on prerelease builds – the developer rows.
@Composable
internal fun GroupedListViews(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
    navigation: Navigation,
) {
    Box(modifier = Modifier.padding(bottom = GroupedFloats.groupedListViewBottomPadding)) {
        GroupedListView(
            listOf(
                inviteFriendsListItem(state, send),
                leaveReviewListItem(state, send),
                changeLanguageListItem(state, navigation),
            ),
        )
    }

    Box(modifier = Modifier.padding(bottom = GroupedFloats.groupedListViewBottomPadding)) {
        GroupedListView(
            listOf(
                sendFeedbackListItem(send),
                clearCachesListItem(state, send),
            ),
        )
    }

    key(state.viewID) {
        Box(modifier = Modifier.padding(bottom = GroupedFloats.groupedListViewBottomPadding)) {
            GroupedListView(
                listOf(
                    blockedUsersListItem(state, send),
                    deleteAccountListItem(state, send),
                    signOutListItem(state, send),
                ),
            )
        }
    }

    state.developerModeListItems?.let { GroupedListView(it) }
}

// MARK: - List Items

/** The list row for the blocked users button. */
internal fun blockedUsersListItem(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.BlockedUsersButtonTapped) },
        innerText = state.blockedUsersButtonText,
        isEnabled = state.isBlockedUsersButtonEnabled,
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.blockedUsersButtonImageBackground,
                    GroupedStrings.BLOCKED_USERS_BUTTON_IMAGE_SYSTEM_NAME,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row that navigates to the change language page. */
internal fun changeLanguageListItem(
    state: SettingsPageReducer.State,
    navigation: Navigation,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Destination {
            navigation.navigate(Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.ChangeLanguage)))
        },
        innerText = state.strings.value(SettingsPageViewStrings.changeLanguage, SettingsPageViewStrings),
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration(
                    backgroundColor = Colors.changeLanguageButtonImageBackground,
                    overlay =
                        SquareIconViewConfiguration.OverlayConfiguration.Resource(
                            resourceID = R.drawable.view_alternate,
                            foregroundColor = Colors.changeLanguageButtonImageForeground,
                            framePercentOfTotalSize = GroupedFloats.CHANGE_LANGUAGE_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE,
                            weight = FontWeight.Bold,
                        ),
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row for the clear caches button. */
internal fun clearCachesListItem(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.ClearCachesButtonTapped) },
        innerText = state.strings.value(SettingsPageViewStrings.clearCachesButtonText, SettingsPageViewStrings),
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.clearCachesButtonImageBackground,
                    GroupedStrings.CLEAR_CACHES_BUTTON_IMAGE_SYSTEM_NAME,
                    GroupedFloats.CLEAR_CACHES_BUTTON_OVERLAY_FRAME_PERCENT_OF_TOTAL_SIZE,
                    FontWeight.Bold,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row for the delete account button. */
internal fun deleteAccountListItem(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.DeleteAccountButtonTapped) },
        innerText = state.strings.value(SettingsPageViewStrings.deleteAccountButtonText, SettingsPageViewStrings),
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.deleteAccountButtonImageBackground,
                    GroupedStrings.DELETE_ACCOUNT_BUTTON_IMAGE_SYSTEM_NAME,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row for the invite friends button. */
internal fun inviteFriendsListItem(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.InviteFriendsButtonTapped) },
        innerText = state.strings.value(SettingsPageViewStrings.inviteFriendsButtonText, SettingsPageViewStrings),
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.inviteFriendsButtonImageBackground,
                    GroupedStrings.INVITE_FRIENDS_BUTTON_IMAGE_SYSTEM_NAME,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row for the leave review button. */
internal fun leaveReviewListItem(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.LeaveReviewButtonTapped) },
        innerText = state.strings.value(SettingsPageViewStrings.leaveReviewButtonText, SettingsPageViewStrings),
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.leaveReviewButtonImageBackground,
                    GroupedStrings.LEAVE_REVIEW_BUTTON_IMAGE_SYSTEM_NAME,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row for the send feedback button. */
internal fun sendFeedbackListItem(send: (SettingsPageReducer.Action) -> Unit): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.SendFeedbackButtonTapped) },
        innerText =
            LocalizedStringKey.SendFeedback
                .localized()
                .lowercase()
                .replaceFirstChar { it.uppercase() },
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.sendFeedbackButtonImageBackground,
                    GroupedStrings.SEND_FEEDBACK_BUTTON_IMAGE_SYSTEM_NAME,
                    weight = FontWeight.SemiBold,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

/** The list row for the sign out button. */
internal fun signOutListItem(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
): ListRowView.Configuration =
    ListRowView.Configuration(
        ListRowView.Interaction.Button { send(SettingsPageReducer.Action.SignOutButtonTapped) },
        innerText = state.strings.value(SettingsPageViewStrings.signOutButtonText, SettingsPageViewStrings),
        imageView = {
            SquareIconImage(
                SquareIconViewConfiguration.symbol(
                    Colors.signOutButtonImageBackground,
                    GroupedStrings.SIGN_OUT_BUTTON_IMAGE_SYSTEM_NAME,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
