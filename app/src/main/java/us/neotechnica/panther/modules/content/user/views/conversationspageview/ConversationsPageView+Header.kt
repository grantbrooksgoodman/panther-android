//
//  ConversationsPageView+Header.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel
import kotlin.math.roundToInt

// MARK: - Constants Accessors

private typealias HeaderColors = ConversationsPageViewColors
private typealias HeaderConstants = ConversationsPageViewConstants
private typealias HeaderFloats = ConversationsPageViewFloats

// MARK: - Header

// The toolbar, title, and search bar. As a search begins, the toolbar
// fades in place and the title fades as it rides up with the search
// bar, until the search bar rests at the top of the page; as the
// search ends, the motion reverses.
@Composable
internal fun Header(
    isSearching: Boolean,
    toolbar: @Composable () -> Unit,
    title: @Composable () -> Unit,
    searchBar: @Composable () -> Unit,
) {
    val searchTransitionFraction =
        animateFloatAsState(
            targetValue = if (isSearching) 1f else 0f,
            animationSpec = SystemAnimation.searchTransition(),
            label = SEARCH_TRANSITION_ANIMATION_LABEL,
        )
    val toolbarAndTitleAlpha =
        animateFloatAsState(
            targetValue = if (isSearching) 0f else 1f,
            animationSpec = SystemAnimation.searchTransitionFade(isBeginning = isSearching),
            label = SEARCH_TRANSITION_FADE_ANIMATION_LABEL,
        )

    Layout(contents = listOf(toolbar, title, searchBar)) { (toolbarMeasurables, titleMeasurables, searchBarMeasurables), constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val toolbarPlaceable = toolbarMeasurables.first().measure(looseConstraints)
        val titlePlaceable = titleMeasurables.first().measure(looseConstraints)
        val searchBarPlaceable = searchBarMeasurables.first().measure(looseConstraints)

        val fraction = searchTransitionFraction.value
        val collapsedHeight = ((toolbarPlaceable.height + titlePlaceable.height) * fraction).roundToInt()
        val searchBarY =
            toolbarPlaceable.height + titlePlaceable.height - collapsedHeight +
                (HeaderFloats.searchTopSpacing.toPx() * fraction).roundToInt()
        val bottomSpacing = (HeaderFloats.searchBottomSpacing.toPx() * (1f - fraction)).roundToInt()

        layout(constraints.maxWidth, searchBarY + searchBarPlaceable.height + bottomSpacing) {
            // Once faded out, the toolbar and title are left unplaced so that
            // they neither draw nor receive taps beneath the search bar.
            val alpha = toolbarAndTitleAlpha.value
            if (alpha > 0f) {
                toolbarPlaceable.placeWithLayer(0, 0) { this.alpha = alpha }
                titlePlaceable.placeWithLayer(0, toolbarPlaceable.height - collapsedHeight) { this.alpha = alpha }
            }

            searchBarPlaceable.place(0, searchBarY)
        }
    }
}

// MARK: - Toolbar

@Composable
internal fun Toolbar(
    state: ConversationsPageReducer.State,
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = HeaderFloats.headerHorizontalPadding,
                    end = HeaderFloats.headerHorizontalPadding,
                    top = HeaderFloats.headerTopPadding,
                ),
    ) {
        ToolbarButton(
            symbolName = HeaderConstants.SETTINGS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME,
            foregroundColor = HeaderColors.settingsToolbarButtonForeground,
            onClick = { viewModel.send(ConversationsPageReducer.Action.SettingsToolbarButtonTapped) },
        )

        if (state.shouldShowExtraToolbarButtons) {
            ToolbarButton(
                symbolName = HeaderConstants.DELETE_CONVERSATIONS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME,
                foregroundColor = HeaderColors.deleteConversationsToolbarButtonForeground,
                onClick = { viewModel.send(ConversationsPageReducer.Action.DeleteConversationsToolbarButtonTapped) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        key(state.composeToolbarButtonViewID) {
            ComposeToolbarButton(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun ComposeToolbarButton(
    state: ConversationsPageReducer.State,
    viewModel: ViewModel<ConversationsPageReducer.State, ConversationsPageReducer.Action>,
) {
    val isAnimated = state.conversations.isEmpty()
    LaunchedEffect(isAnimated) {
        if (isAnimated) viewModel.send(ConversationsPageReducer.Action.AnimatedComposeToolbarButtonAppeared)
    }

    // Once the reducer advances the amount past rest, the button pulses
    // between rest and the amount until the list fills.
    val scale =
        if (isAnimated && state.animationAmount != 1f) {
            rememberInfiniteTransition(label = COMPOSE_ANIMATION_LABEL)
                .animateFloat(
                    initialValue = 1f,
                    targetValue = state.animationAmount,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                tween(
                                    durationMillis =
                                        (HeaderFloats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_DURATION * MILLISECONDS_PER_SECOND)
                                            .toInt(),
                                    delayMillis =
                                        (HeaderFloats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_DELAY * MILLISECONDS_PER_SECOND)
                                            .toInt(),
                                    easing = LinearEasing,
                                ),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = COMPOSE_ANIMATION_LABEL,
                ).value
        } else {
            1f
        }

    ToolbarButton(
        symbolName = HeaderConstants.COMPOSE_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME,
        foregroundColor = HeaderColors.composeToolbarButtonForeground,
        onClick = { viewModel.send(ConversationsPageReducer.Action.ComposeToolbarButtonTapped) },
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun ToolbarButton(
    symbolName: String,
    foregroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.defaultMinSize(
                minWidth = HeaderFloats.toolbarButtonFrameMinWidth,
                minHeight = HeaderFloats.toolbarButtonFrameMinHeight,
            ),
    ) {
        Components.Button(symbolName = symbolName, foregroundColor = foregroundColor, onClick = onClick)
    }
}

private const val COMPOSE_ANIMATION_LABEL = "ComposeToolbarButton"
private const val MILLISECONDS_PER_SECOND = 1000f
private const val SEARCH_TRANSITION_ANIMATION_LABEL = "ConversationsPageSearchTransition"
private const val SEARCH_TRANSITION_FADE_ANIMATION_LABEL = "ConversationsPageSearchTransitionFade"
