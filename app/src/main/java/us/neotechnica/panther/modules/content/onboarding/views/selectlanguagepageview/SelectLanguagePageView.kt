//
//  SelectLanguagePageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.onboarding.views.selectlanguagepageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.OnboardingBackButton
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingBackButton(
                text = state.strings.value(SelectLanguagePageViewStrings.backButtonText),
                isEnabled = true,
                onClick = { viewModel.send(SelectLanguagePageReducer.Action.BackButtonTapped) },
            )

            InstructionView(state.instructionViewStrings, modifier = Modifier.fillMaxWidth())

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.languages) { language ->
                    val isSelected = language == state.selectedLanguageName
                    Components.Text(
                        language,
                        color = if (isSelected) colors.background else colors.titleText,
                        font = Font.systemMedium(FontScale.Small),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent else colors.groupedContentBackground)
                                .clickable {
                                    viewModel.send(SelectLanguagePageReducer.Action.SelectedLanguageNameChanged(language))
                                }.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }

            Components.CapsuleButton(
                text = state.strings.value(SelectLanguagePageViewStrings.continueButtonText),
                onClick = { viewModel.send(SelectLanguagePageReducer.Action.ContinueButtonTapped) },
                primary = true,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
