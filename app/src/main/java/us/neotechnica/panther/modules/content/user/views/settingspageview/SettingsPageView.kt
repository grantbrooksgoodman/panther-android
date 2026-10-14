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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.bundle.navigators.SettingsNavigatorState
import us.neotechnica.panther.bundle.navigators.SettingsRoute
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.components.PageSheetView
import us.neotechnica.panther.designsystem.modules.foundation.constants.StackFloats
import us.neotechnica.panther.designsystem.modules.foundation.extensions.doneButton
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.components.ContactDetailView
import us.neotechnica.panther.modules.content.user.components.rememberContactCardPresenter
import us.neotechnica.panther.modules.content.user.constants.SettingsPageViewFloats
import us.neotechnica.panther.modules.content.user.models.BuildInfoButtonStrings
import us.neotechnica.panther.modules.content.user.views.inviteqrcodepageview.InviteQRCodePageView
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

// MARK: - Constants Accessors

private typealias Floats = SettingsPageViewFloats

/**
 * The settings page: the current user's contact detail view pinned
 * above the scrolling grouped lists, with the cycling build-info button
 * pinned at the bottom.
 *
 * @param modifier The modifier for this view.
 */
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
    val navigatorStates by navigation.state.collectAsState()
    val presentContactCard = rememberContactCardPresenter()

    // The page is dismissed only with its header's Done item; the
    // system back gesture is ignored.
    BackHandler {}

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        StatefulView(state = state.viewState) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                HeaderView(
                    centerItem =
                        HeaderView.CenterItemType.Text(
                            HeaderView.TextAttributes(state.navigationTitle, foregroundColor = colors.navigationBarTitle),
                        ),
                    rightItem =
                        HeaderView.PeripheralButtonType.doneButton(
                            action = { viewModel.send(SettingsPageReducer.Action.DoneToolbarButtonTapped) },
                            foregroundColor = colors.navigationBarButton,
                        ),
                    attributes = HeaderView.Attributes(windowInsets = WindowInsets.statusBars),
                )

                // The contact detail view opens the current user's device
                // contact when one resolved.
                val contactPair = state.contactPair
                val contactTapModifier =
                    if (contactPair == null) {
                        Modifier
                    } else {
                        Modifier.clickable {
                            presentContactCard.onTap(
                                DependencyValues.current.clientSession.entity.user.currentUser
                                    ?.phoneNumber,
                                contactPair.contact.fullName,
                            )
                        }
                    }

                ContactDetailView(
                    titleLabelText = state.contactDetailViewTitleLabelText,
                    subtitleLabelText = state.contactDetailViewSubtitleLabelText,
                    image = state.contactDetailViewImage,
                    modifier =
                        contactTapModifier
                            .padding(bottom = Floats.contactDetailViewBottomPadding)
                            .padding(horizontal = Floats.contactDetailViewHorizontalPadding)
                            .padding(top = Floats.contactDetailViewTopPadding),
                )

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Floats.groupedListViewHorizontalPadding),
                ) {
                    key(state.groupedListViewsID) {
                        GroupedListViews(state, viewModel::send, navigation)
                    }
                }

                BuildInfoButton(
                    state = state,
                    onTap = { viewModel.send(SettingsPageReducer.Action.BuildInfoButtonTapped) },
                    onLongPress = { viewModel.send(SettingsPageReducer.Action.LongPressGestureRecognized) },
                )
            }
        }

        PageSheetView(
            isPresented = navigatorStates.settings.sheet == SettingsNavigatorState.SheetPaths.InviteQRCode,
            onDismissRequest = { navigation.navigate(Route.Settings(SettingsRoute.Sheet(null))) },
        ) {
            InviteQRCodePageView()
        }
    }
}

// MARK: - Build Info Button

// The version or copyright notice, preceded in its copyright form by
// the company logo. Tapping cycles the notice; a long press copies it
// or offers prerelease mode.
@Composable
private fun BuildInfoButton(
    state: SettingsPageReducer.State,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = StackFloats.defaultSpacing)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
                },
    ) {
        if (state.buildInfoButtonStrings.key == BuildInfoButtonStrings.BuildInfoButtonStringKey.COPYRIGHT) {
            val image = if (isDarkModeActive) state.buildInfoButtonDarkBackgroundImage else state.buildInfoButtonLightBackgroundImage
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(width = Floats.buildInfoButtonImageFrameWidth, height = Floats.buildInfoButtonImageFrameHeight)
                        .padding(bottom = Floats.buildInfoButtonImageBottomPadding),
            )
        }

        Components.Text(
            state.buildInfoButtonStrings.labelText,
            foregroundColor = colors.subtitleText,
            font = Font.system(FontScale.Small),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Floats.buildInfoButtonLabelBottomPadding),
        )
    }
}
