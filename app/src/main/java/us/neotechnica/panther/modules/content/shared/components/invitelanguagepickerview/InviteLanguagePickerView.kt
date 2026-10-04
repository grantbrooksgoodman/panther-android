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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.shared.constants.InviteLanguagePickerViewFloats
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = InviteLanguagePickerViewFloats

/**
 * The invite language picker: a searchable language list for choosing
 * the language of an invitation message.
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

    val isNoResults = state.queriedLanguageNames.isEmpty() && state.searchQuery.isNotBlank()
    val languages =
        (if (state.queriedLanguageNames.isEmpty()) state.localizedLanguageNames else state.queriedLanguageNames)
            .entries
            .sortedBy { it.value }

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Header(
                state = state,
                onCancel = { viewModel.send(InviteLanguagePickerReducer.Action.CancelHeaderItemTapped) },
                onDone = { viewModel.send(InviteLanguagePickerReducer.Action.DoneHeaderItemTapped) },
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.send(InviteLanguagePickerReducer.Action.SearchQueryChanged(it)) },
                singleLine = true,
                placeholder = { Components.Text(LocalizedStringKey.Search.localized(), color = colors.subtitleText) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.horizontalPadding, vertical = Floats.rowVerticalPadding),
            )

            if (isNoResults) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Components.Text(state.noResultsLabelText, color = colors.subtitleText)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(languages, key = { it.key }) { entry ->
                        LanguageRow(
                            name = entry.value,
                            isSelected = entry.key == state.selectedLanguageCode,
                            onClick = { viewModel.send(InviteLanguagePickerReducer.Action.SelectedLanguageCodeChanged(entry.key)) },
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Header

@Composable
private fun Header(
    state: InviteLanguagePickerReducer.State,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.horizontalPadding, vertical = Floats.rowVerticalPadding)) {
        CircleChipButton(
            systemName = "xmark",
            contentDescription = state.cancelHeaderItemText,
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterStart),
            tint = colors.titleText,
        )
        Components.Text(
            state.navigationTitle,
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        CircleChipButton(
            systemName = "checkmark",
            contentDescription = state.doneHeaderItemText,
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterEnd),
            tint = if (state.isDoneHeaderItemEnabled) colors.titleText else colors.subtitleText,
            enabled = state.isDoneHeaderItemEnabled,
        )
    }
}

// MARK: - Language Row

@Composable
private fun LanguageRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = Floats.horizontalPadding, vertical = Floats.rowVerticalPadding),
    ) {
        Components.Text(name, color = colors.titleText, modifier = Modifier.weight(1f))
        if (isSelected) {
            Components.Symbol("checkmark", color = colors.titleText, modifier = Modifier.size(Floats.checkmarkSize))
        }
    }
}
