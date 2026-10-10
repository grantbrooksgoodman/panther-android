//
//  InviteLanguagePickerView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.invitelanguagepickerview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelButton
import us.neotechnica.panther.designsystem.modules.foundation.extensions.doneButton
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.components.SearchBarInView
import us.neotechnica.panther.modules.content.shared.constants.InviteLanguagePickerViewColors
import us.neotechnica.panther.modules.content.shared.constants.InviteLanguagePickerViewFloats
import us.neotechnica.panther.modules.content.shared.constants.InviteLanguagePickerViewStrings
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Colors = InviteLanguagePickerViewColors
private typealias Floats = InviteLanguagePickerViewFloats
private typealias Strings = InviteLanguagePickerViewStrings

/**
 * A sheet that lets the user choose the language for an invitation
 * message.
 *
 * Use `InviteLanguagePickerView` when inviting someone to the app. The
 * sheet displays a searchable list of the supported languages;
 * selecting one enables the done button, which confirms the choice and
 * begins composing the invitation in that language.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun InviteLanguagePickerView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(InviteLanguagePickerReducer.State(), InviteLanguagePickerReducer()) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(InviteLanguagePickerReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(Unit) { viewModel.send(InviteLanguagePickerReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        HeaderView(
            leftItem =
                HeaderView.PeripheralButtonType.cancelButton(foregroundColor = colors.navigationBarButton) {
                    viewModel.send(InviteLanguagePickerReducer.Action.CancelHeaderItemTapped)
                },
            centerItem =
                HeaderView.CenterItemType.Text(
                    HeaderView.TextAttributes(state.navigationTitle, foregroundColor = colors.navigationBarTitle),
                ),
            rightItem =
                HeaderView.PeripheralButtonType.doneButton(
                    foregroundColor = if (state.isDoneHeaderItemEnabled) colors.navigationBarButton else colors.disabled,
                    isEnabled = state.isDoneHeaderItemEnabled,
                ) {
                    viewModel.send(InviteLanguagePickerReducer.Action.DoneHeaderItemTapped)
                },
            attributes = HeaderView.Attributes(showsDivider = false, sizeClass = HeaderView.SizeClass.Sheet),
        ) {
            SearchBarInView(
                query = state.searchQuery,
                onQueryChange = { viewModel.send(InviteLanguagePickerReducer.Action.SearchQueryChanged(it)) },
            ) {
                Box(modifier = Modifier.fillMaxSize().background(colors.groupedContentBackground)) {
                    if (state.queriedLanguageNames.isEmpty() && state.searchQuery.isNotBlank()) {
                        Components.Text(
                            state.noResultsLabelText,
                            foregroundColor = colors.subtitleText,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        ListView(
                            languageNames =
                                if (state.queriedLanguageNames.isEmpty()) state.localizedLanguageNames else state.queriedLanguageNames,
                            selectedLanguageCode = state.selectedLanguageCode,
                            onLanguageCodeSelected = {
                                viewModel.send(InviteLanguagePickerReducer.Action.SelectedLanguageCodeChanged(it))
                            },
                        )
                    }
                }
            }
        }
    }
}

// MARK: - List

@Composable
private fun ListView(
    languageNames: Map<String, String>,
    selectedLanguageCode: String,
    onLanguageCodeSelected: (String) -> Unit,
) {
    val colors = LocalPantherColors.current
    val keys = languageNames.entries.sortedBy { it.value }.map { it.key }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(keys, key = { it }) { key ->
            val isSelected = key == selectedLanguageCode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.groupedRowBackground)
                        .clickable { onLanguageCodeSelected(key) }
                        .padding(
                            horizontal = Floats.listViewCellHorizontalPadding,
                            vertical = Floats.listViewCellVerticalPadding,
                        ),
            ) {
                Components.Text(
                    languageNames.getValue(key),
                    foregroundColor = colors.titleText,
                    font = if (isSelected) Font.systemSemibold() else Font.system,
                )

                Spacer(Modifier.weight(1f))

                if (isSelected) {
                    Components.Symbol(Strings.SELECTED_CELL_IMAGE_SYSTEM_NAME, foregroundColor = Colors.selectedCellImageForeground)
                }
            }
        }
    }
}
