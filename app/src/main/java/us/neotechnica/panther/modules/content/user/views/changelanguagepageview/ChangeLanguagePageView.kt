//
//  ChangeLanguagePageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.changelanguagepageview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import us.neotechnica.panther.designsystem.modules.foundation.views.StatefulView
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.onboarding.components.InstructionView
import us.neotechnica.panther.modules.content.onboarding.components.WheelPicker
import us.neotechnica.panther.modules.content.user.constants.ChangeLanguagePageViewFloats
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.UserContentRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias Floats = ChangeLanguagePageViewFloats

/**
 * The page for changing the language the app translates content into,
 * pushed from Settings.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun ChangeLanguagePageView(modifier: Modifier = Modifier) {
    val viewModel = remember { ViewModel(ChangeLanguagePageReducer.State(), ChangeLanguagePageReducer()) }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.send(ChangeLanguagePageReducer.Action.ViewDisappeared)
            viewModel.close()
        }
    }
    LaunchedEffect(Unit) { viewModel.send(ChangeLanguagePageReducer.Action.ViewAppeared) }

    val state by viewModel.state.collectAsState()
    val colors = LocalPantherColors.current

    StatefulView(
        state = state.viewState,
        modifier = modifier.background(colors.groupedContentBackground),
        exceptionRetryHandler = { viewModel.send(ChangeLanguagePageReducer.Action.ViewAppeared) },
    ) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Header(
                title = state.strings.value(ChangeLanguagePageViewStrings.navigationTitle),
                onBack = { DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop)) },
            )

            InstructionView(state.instructionViewStrings)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = Floats.innerVStackTopPadding),
            ) {
                WheelPicker(
                    items = state.languages,
                    selectedIndex = state.languages.indexOf(state.selectedLanguageName),
                    onSelectedIndexChange = { index ->
                        state.languages.getOrNull(index)?.let {
                            viewModel.send(ChangeLanguagePageReducer.Action.SelectedLanguageNameChanged(it))
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Floats.pickerHorizontalPadding)
                            .padding(bottom = Floats.headerVerticalPadding),
                )

                Components.CapsuleButton(
                    text = state.strings.value(ChangeLanguagePageViewStrings.confirmButtonText),
                    action = { viewModel.send(ChangeLanguagePageReducer.Action.ConfirmButtonTapped) },
                    isEnabled = state.isConfirmButtonEnabled,
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// MARK: - Header

@Composable
private fun Header(
    title: String,
    onBack: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding),
    ) {
        Components.Text(
            title,
            foregroundColor = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        CircleChipButton(
            systemName = "chevron.backward",
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            tint = colors.titleText,
            glyphSize = Floats.doneButtonGlyphSize,
        )
    }
}
