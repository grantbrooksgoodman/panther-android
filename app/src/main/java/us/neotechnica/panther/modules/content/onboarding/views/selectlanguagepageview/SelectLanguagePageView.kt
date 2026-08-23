//
//  SelectLanguagePageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.WheelPicker
import us.neotechnica.panther.modules.content.onboarding.constants.SelectLanguagePageViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = SelectLanguagePageViewFloats

/**
 * The onboarding page for selecting the user's native language.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun SelectLanguagePageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(SelectLanguagePageReducer.State(), SelectLanguagePageReducer()) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    LaunchedEffect(Unit) { viewModel.send(SelectLanguagePageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(
        state = state.viewState,
        modifier = modifier,
        onRetry = { viewModel.send(SelectLanguagePageReducer.Action.ViewAppeared) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InstructionView(state.instructionViewStrings)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = Floats.innerVStackBottomPadding),
            ) {
                Components.Text(
                    state.strings.value(SelectLanguagePageViewStrings.instructionLabelText),
                    color = colors.subtitleText,
                    font = Font.systemSemibold(),
                    modifier = Modifier.padding(vertical = Floats.instructionLabelVerticalPadding),
                )

                WheelPicker(
                    items = state.languages,
                    selectedIndex = state.languages.indexOf(state.selectedLanguageName),
                    onSelectedIndexChange = { index ->
                        state.languages.getOrNull(index)?.let {
                            viewModel.send(SelectLanguagePageReducer.Action.SelectedLanguageNameChanged(it))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.pickerHorizontalPadding),
                )

                Components.CapsuleButton(
                    text = state.strings.value(SelectLanguagePageViewStrings.continueButtonText),
                    onClick = { viewModel.send(SelectLanguagePageReducer.Action.ContinueButtonTapped) },
                    primary = true,
                    modifier = Modifier.padding(vertical = Floats.continueButtonVerticalPadding),
                )

                Components.Button(
                    text = state.strings.value(SelectLanguagePageViewStrings.backButtonText),
                    color = colors.titleText,
                    onClick = { viewModel.send(SelectLanguagePageReducer.Action.BackButtonTapped) },
                    font = Font.system(FontScale.Custom(Floats.BACK_BUTTON_LABEL_FONT_SIZE)),
                    modifier = Modifier.padding(top = Floats.backButtonTopPadding),
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
