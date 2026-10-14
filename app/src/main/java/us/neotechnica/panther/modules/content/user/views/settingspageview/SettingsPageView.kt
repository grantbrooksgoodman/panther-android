//
//  SettingsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.components.PageSheetView
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.components.rememberContactCardPresenter
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.components.AvatarImageView
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.models.BuildInfoButtonStrings
import us.neotechnica.panther.modules.content.user.services.DeveloperModeListItem
import us.neotechnica.panther.modules.content.user.views.inviteqrcodepageview.InviteQRCodePageView
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.Navigation
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.SettingsNavigatorState
import us.neotechnica.panther.navigation.SettingsRoute
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Floats = SettingsPageViewFloats
private typealias PageStrings = SettingsPageViewConstants

/**
 * The settings page: the current user's contact header followed by
 * grouped action cards and the cycling build-info button.
 *
 * @param modifier The modifier for this view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageView(modifier: Modifier = Modifier) {
    val viewModel =
        remember {
            ViewModel(SettingsPageReducer.State(), SettingsPageReducer())
                .observing(DependencyValues.current.sharedEvents.traitCollectionChanged.events) {
                    SettingsPageReducer.Action.TraitCollectionChanged
                }
        }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(SettingsPageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(Unit) { viewModel.send(SettingsPageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current
    val navigation = remember { DependencyValues.current.navigation }
    val navState by navigation.state.collectAsState()
    val presentContactCard = rememberContactCardPresenter()

    // The page is dismissed only with its navigation bar button; the
    // system back gesture is ignored.
    BackHandler {}

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        StatefulView(state = state.viewState) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                Header(
                    title = state.navigationTitle,
                    doneText = state.doneToolbarButtonText,
                    onDone = { viewModel.send(SettingsPageReducer.Action.DoneToolbarButtonTapped) },
                )

                // The header and contact card stay pinned while the grouped
                // lists beneath them scroll; the build information sits at
                // the bottom of the page.
                ContactDetailCard(
                    state = state,
                    onTap = { phoneNumber, name -> presentContactCard.onTap(phoneNumber, name) },
                    modifier = Modifier.padding(top = Floats.contentTopPadding),
                )

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                ) {
                    SettingsActionCards(state = state, send = viewModel::send, navigation = navigation)
                    DeveloperModeCards(items = state.developerModeListItems)
                }

                BuildInfoButton(
                    strings = state.buildInfoButtonStrings,
                    onTap = { viewModel.send(SettingsPageReducer.Action.BuildInfoButtonTapped) },
                    onLongPress = { viewModel.send(SettingsPageReducer.Action.LongPressGestureRecognized) },
                )
            }
        }

        PageSheetView(
            isPresented = navState.settings.sheet == SettingsNavigatorState.SheetPaths.InviteQRCode,
            onDismissRequest = { navigation.navigate(Route.Settings(SettingsRoute.Sheet(null))) },
        ) {
            InviteQRCodePageView()
        }
    }
}

// MARK: - Header

// The navigation bar: the centered title and the trailing Done
// button, over a background that extends beneath the status bar.
@Composable
private fun Header(
    title: String,
    doneText: String,
    onDone: () -> Unit,
) {
    val colors = LocalPantherColors.current

    HeaderView(
        centerItem =
            HeaderView.CenterItemType.Text(
                HeaderView.TextAttributes(title, foregroundColor = colors.navigationBarTitle),
            ),
        rightItem =
            HeaderView.PeripheralButtonType.Text(
                HeaderView.TextButtonAttributes(
                    text = HeaderView.TextAttributes(doneText, foregroundColor = colors.navigationBarButton),
                    action = onDone,
                ),
            ),
        attributes = HeaderView.Attributes(windowInsets = WindowInsets.statusBars),
    )
}

// MARK: - Contact Detail

@Composable
private fun ContactDetailCard(
    state: SettingsPageReducer.State,
    onTap: (PhoneNumber?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val phoneNumber = UserSessionService.currentUser?.phoneNumber
    val contactName = state.contactPair?.contact?.fullName
    val title = state.contactDetailViewTitleLabelText.ifEmpty { phoneNumber?.formattedString() ?: LocalizedStringKey.You.localized() }
    val subtitle = state.contactDetailViewSubtitleLabelText?.takeIf { it.isNotEmpty() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalMargin, vertical = Floats.cardVerticalMargin)
                .clip(RoundedCornerShape(Floats.contactCornerRadius))
                .background(colors.groupedRowBackground)
                .clickable { onTap(phoneNumber, contactName) }
                .padding(Floats.cardPadding),
    ) {
        AvatarImageView(
            remember(state.contactPair) {
                state.contactPair
                    ?.contact
                    ?.image
                    ?.asImageBitmap()
            },
            size = DpSize(Floats.avatarSize, Floats.avatarSize),
        )
        Column(modifier = Modifier.weight(1f).padding(start = Floats.contactNameStartPadding)) {
            Components.Text(title, foregroundColor = colors.titleText, font = Font.systemSemibold())
            subtitle?.let {
                Components.Text(it, foregroundColor = colors.subtitleText, font = Font.system(FontScale.Small))
            }
        }
        if (subtitle != null) {
            Components.Symbol(
                PageStrings.CHEVRON_IMAGE_SYSTEM_NAME,
                foregroundColor = colors.subtitleText,
                weight = FontWeight.SemiBold,
                modifier =
                    Modifier.size(
                        if (SFSymbol.resolvesToSFSymbol(PageStrings.CHEVRON_IMAGE_SYSTEM_NAME)) {
                            Floats.contactChevronSymbolSize
                        } else {
                            Floats.contactChevronSize
                        },
                    ),
            )
        }
    }
}

// MARK: - Action Cards

@Composable
private fun SettingsActionCards(
    state: SettingsPageReducer.State,
    send: (SettingsPageReducer.Action) -> Unit,
    navigation: Navigation,
) {
    SettingsCard {
        InviteFriendsListItem(state.strings.value(SettingsPageViewStrings.inviteFriendsButtonText)) {
            send(SettingsPageReducer.Action.InviteFriendsButtonTapped)
        }
        SettingsRowDivider()
        LeaveReviewListItem(state.strings.value(SettingsPageViewStrings.leaveReviewButtonText)) {
            send(SettingsPageReducer.Action.LeaveReviewButtonTapped)
        }
        SettingsRowDivider()
        ChangeLanguageListItem(state.strings.value(SettingsPageViewStrings.changeLanguage)) {
            navigation.navigate(
                Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.ChangeLanguage)),
            )
        }
    }

    val sendFeedbackTitle =
        LocalizedStringKey.SendFeedback
            .localized()
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    SettingsCard {
        SendFeedbackListItem(sendFeedbackTitle) {
            send(SettingsPageReducer.Action.SendFeedbackButtonTapped)
        }
        SettingsRowDivider()
        ClearCachesListItem(state.strings.value(SettingsPageViewStrings.clearCachesButtonText)) {
            send(SettingsPageReducer.Action.ClearCachesButtonTapped)
        }
    }

    SettingsCard {
        BlockedUsersListItem(state.blockedUsersButtonText, state.isBlockedUsersButtonEnabled) {
            send(SettingsPageReducer.Action.BlockedUsersButtonTapped)
        }
        SettingsRowDivider()
        DeleteAccountListItem(state.strings.value(SettingsPageViewStrings.deleteAccountButtonText)) {
            send(SettingsPageReducer.Action.DeleteAccountButtonTapped)
        }
        SettingsRowDivider()
        SignOutListItem(state.strings.value(SettingsPageViewStrings.signOutButtonText)) {
            send(SettingsPageReducer.Action.SignOutButtonTapped)
        }
    }
}

// MARK: - Developer Mode

@Composable
private fun DeveloperModeCards(items: List<DeveloperModeListItem>?) {
    if (items.isNullOrEmpty()) return
    SettingsCard {
        items.forEachIndexed { index, item ->
            if (index > 0) SettingsRowDivider()
            SettingsListRow(configuration = item.configuration, title = item.title, onClick = item.action)
        }
    }
}

// MARK: - Build Info Button

// The version or copyright notice, preceded in its copyright form by
// the company logo. Tapping cycles the notice; a long press reveals
// developer options.
@Composable
private fun BuildInfoButton(
    strings: BuildInfoButtonStrings,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = LocalPantherColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
                }.padding(top = Floats.versionTopPadding, bottom = Floats.versionBottomPadding),
    ) {
        if (strings.key == BuildInfoButtonStrings.BuildInfoButtonStringKey.COPYRIGHT) {
            Image(
                colorFilter = ColorFilter.tint(colors.titleText),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(bottom = Floats.buildInfoButtonImageBottomPadding)
                        .size(width = Floats.buildInfoButtonImageFrameWidth, height = Floats.buildInfoButtonImageFrameHeight),
                painter = painterResource(R.drawable.nt_black),
            )
        }

        Components.Text(
            strings.labelText,
            foregroundColor = colors.subtitleText,
            font = Font.system(FontScale.Small),
            textAlign = TextAlign.Center,
        )
    }
}

// MARK: - Cards

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = LocalPantherColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.cardHorizontalMargin, vertical = Floats.cardVerticalMargin)
                .clip(RoundedCornerShape(Floats.cardCornerRadius))
                .background(colors.groupedRowBackground),
    ) {
        content()
    }
}
