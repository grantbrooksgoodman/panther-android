//
//  ContactSelectorPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelButton
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.components.SearchBarInView
import us.neotechnica.panther.modules.content.user.components.ContactPairListView
import us.neotechnica.panther.modules.content.user.constants.ContactSelectorPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ContactSelectorPageViewFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = ContactSelectorPageViewColors
private typealias Floats = ContactSelectorPageViewFloats

/**
 * A contact picker: search the user's known contact pairs, or enter a
 * phone number to look up its registered user. Presented as a sheet
 * either from the chat info page – to add a participant – or from the
 * new chat page – to choose a recipient.
 *
 * The page insets its own content from the navigation bar and keyboard,
 * so present it in a sheet that ignores the bottom safe area.
 */
object ContactSelectorPageView {
    // MARK: - Types

    /** The context the page was presented from. */
    enum class EntryPoint {
        CHAT_INFO_PAGE_VIEW,
        NEW_CHAT_PAGE_VIEW,
    }

    // MARK: - Body

    /**
     * Renders the contact selector page.
     *
     * @param entryPoint The context the page was presented from.
     * @param modifier The modifier for this view.
     */
    @Composable
    operator fun invoke(
        entryPoint: EntryPoint,
        modifier: Modifier = Modifier,
    ) {
        val viewModel =
            remember(entryPoint) {
                ViewModel(ContactSelectorPageReducer.State(entryPoint), ContactSelectorPageReducer())
            }
        DisposableEffect(viewModel) {
            onDispose {
                viewModel.send(ContactSelectorPageReducer.Action.ViewDisappeared)
                viewModel.close()
            }
        }
        LaunchedEffect(Unit) { viewModel.send(ContactSelectorPageReducer.Action.ViewAppeared) }

        val state by viewModel.state.collectAsState()
        val colors = LocalPantherColors.current

        StatefulView(state = state.viewState, modifier = modifier.background(colors.groupedContentBackground)) {
            HeaderView(
                leftItem =
                    if (state.shouldShowInviteButton) {
                        HeaderView.PeripheralButtonType.Text(
                            HeaderView.TextButtonAttributes(
                                text =
                                    HeaderView.TextAttributes(
                                        state.inviteToolbarButtonText,
                                        foregroundColor = colors.navigationBarButton,
                                    ),
                            ) {
                                viewModel.send(ContactSelectorPageReducer.Action.InviteToolbarButtonTapped)
                            },
                        )
                    } else {
                        null
                    },
                centerItem =
                    HeaderView.CenterItemType.Text(
                        HeaderView.TextAttributes(state.navigationTitle, foregroundColor = colors.navigationBarTitle),
                    ),
                rightItem =
                    HeaderView.PeripheralButtonType.cancelButton(
                        font = Font.system(FontScale.Custom(Floats.CANCEL_TOOLBAR_BUTTON_SYSTEM_FONT_SIZE)),
                        foregroundColor = colors.navigationBarButton,
                    ) {
                        viewModel.send(ContactSelectorPageReducer.Action.CancelToolbarButtonTapped)
                    },
                attributes = HeaderView.Attributes(showsDivider = false, sizeClass = HeaderView.SizeClass.Sheet),
            ) {
                SearchBarInView(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.send(ContactSelectorPageReducer.Action.SearchQueryChanged(it)) },
                    keyboardType = if (entryPoint == EntryPoint.CHAT_INFO_PAGE_VIEW) KeyboardType.Phone else null,
                    placeholderText = state.searchBarPlaceholderText,
                ) {
                    // The page extends beneath the navigation bar and keyboard, as
                    // the presenting sheet ignores the bottom safe area: the list
                    // scrolls its last row clear of them, and the no results label
                    // centers above them.
                    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(if (isDarkModeActive) Colors.listViewDarkBackground else Colors.listViewLightBackground),
                    ) {
                        if (state.queriedContactPairs.isNotEmpty()) {
                            ContactPairListView(
                                sections = state.sections,
                                onSelect = { viewModel.send(ContactSelectorPageReducer.Action.SelectedContactPairChanged(it)) },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                            ) {
                                NoResultsView(
                                    text = state.noResultsLabelText,
                                    onFindUser = { viewModel.send(ContactSelectorPageReducer.Action.FindUserButtonTapped) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - No Results View

@Composable
private fun NoResultsView(
    text: String,
    onFindUser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (text == LocalizedStringKey.NoResults.localized()) {
        Components.Text(
            text,
            foregroundColor = Colors.noResultsLabelForeground,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(horizontal = Floats.noResultsLabelHorizontalPadding),
        )
    } else {
        Components.Button(
            text = text,
            foregroundColor = Colors.noResultsLabelAlternateForeground,
            onClick = onFindUser,
            modifier = modifier.padding(horizontal = Floats.noResultsLabelHorizontalPadding),
        )
    }
}
