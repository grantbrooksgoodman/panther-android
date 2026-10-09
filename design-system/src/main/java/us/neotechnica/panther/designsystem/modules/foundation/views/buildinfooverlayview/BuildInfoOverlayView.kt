//
//  BuildInfoOverlayView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.views.buildinfooverlayview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontType
import us.neotechnica.panther.designsystem.modules.foundation.constants.BuildInfoOverlayViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.BuildInfoOverlayViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.constants.BuildInfoOverlayViewStrings
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Type Aliases

private typealias BuildInfoOverlayViewModel =
    ViewModel<BuildInfoOverlayReducer.State, BuildInfoOverlayReducer.Action>

// MARK: - Constants Accessors

private typealias Colors = BuildInfoOverlayViewColors
private typealias Floats = BuildInfoOverlayViewFloats
private typealias Strings = BuildInfoOverlayViewStrings

// MARK: - Body

@Composable
internal fun BuildInfoOverlayView(viewModel: BuildInfoOverlayViewModel) {
    val state by viewModel.state.collectAsState()

    // Reading the presented alert recomposes the buttons' enabled
    // state whenever an alert appears or disappears.
    val presentedAlert by AlertPresenter.current.collectAsState()
    val isUserInteractionDisabled = presentedAlert != null || state.isUserInteractionDisabled

    val backgroundColor by animateColorAsState(
        targetValue = state.backgroundColor,
        animationSpec =
            tween(
                durationMillis = (DEFAULT_ANIMATION_DURATION_MILLISECONDS / Floats.TRANSLUCENCY_ANIMATION_SPEED).toInt(),
                easing = EaseIn,
            ),
        label = "BuildInfoOverlayBackgroundColor",
    )

    LaunchedEffect(viewModel) {
        viewModel.send(BuildInfoOverlayReducer.Action.ViewAppeared)
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier =
            Modifier.offset(
                x = Floats.X_OFFSET.dp,
                y = state.yOffset.dp,
            ),
    ) {
        SendFeedbackButton(
            state,
            backgroundColor = backgroundColor,
            isUserInteractionDisabled = isUserInteractionDisabled,
        ) {
            viewModel.send(BuildInfoOverlayReducer.Action.SendFeedbackButtonTapped)
        }

        StatsView(state, backgroundColor = backgroundColor)

        BuildInfoButton(
            state,
            backgroundColor = backgroundColor,
            isUserInteractionDisabled = isUserInteractionDisabled,
        ) {
            viewModel.send(BuildInfoOverlayReducer.Action.BuildInfoButtonTapped)
        }
    }
}

// MARK: - Auxiliary

@Composable
private fun BuildInfoButton(
    state: BuildInfoOverlayReducer.State,
    backgroundColor: Color,
    isUserInteractionDisabled: Boolean,
    onClick: () -> Unit,
) {
    // The indicator's negative trailing padding narrows the
    // default stack spacing between the indicator and the label.
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(
                (DEFAULT_STACK_SPACING + Floats.DEVELOPER_MODE_INDICATOR_TRAILING_PADDING).dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(Floats.BUILD_INFO_BUTTON_FRAME_HEIGHT.dp)
                .background(backgroundColor)
                .padding(horizontal = 1.dp)
                .clickable(
                    enabled = !isUserInteractionDisabled,
                    onClick = onClick,
                ),
    ) {
        if (state.isDeveloperModeEnabled) {
            Box(
                modifier =
                    Modifier
                        .size(
                            width = Floats.DEVELOPER_MODE_INDICATOR_FRAME_WIDTH.dp,
                            height = Floats.DEVELOPER_MODE_INDICATOR_FRAME_HEIGHT.dp,
                        ).clip(CircleShape)
                        .background(state.developerModeIndicatorDotColor),
            )
        }

        Components.Text(
            state.buildInfoButtonText,
            foregroundColor = Colors.buildInfoButtonLabelForeground,
            font = Font.systemBold(FontScale.Small),
        )
    }
}

@Composable
private fun SendFeedbackButton(
    state: BuildInfoOverlayReducer.State,
    backgroundColor: Color,
    isUserInteractionDisabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .height(Floats.SEND_FEEDBACK_BUTTON_FRAME_HEIGHT.dp)
                .background(backgroundColor)
                .padding(horizontal = 1.dp)
                .clickable(
                    enabled = !isUserInteractionDisabled,
                    onClick = onClick,
                ),
    ) {
        Components.Text(
            state.sendFeedbackButtonText,
            foregroundColor = Colors.sendFeedbackButtonLabelForeground,
            font =
                Font(
                    FontType.Custom(
                        name = Strings.SEND_FEEDBACK_BUTTON_LABEL_FONT_NAME,
                        isUnderlined = true,
                    ),
                    FontScale.Custom(Floats.SEND_FEEDBACK_BUTTON_LABEL_FONT_SIZE),
                ),
        )
    }
}

@Composable
private fun StatsView(
    state: BuildInfoOverlayReducer.State,
    backgroundColor: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .height(Floats.STATS_VIEW_FRAME_HEIGHT.dp)
                .background(backgroundColor)
                .padding(horizontal = 1.dp),
    ) {
        Components.Text(
            state.statsLabelText,
            foregroundColor = Colors.statsLabelForeground,
            font = Font.system(FontScale.Small),
        )
    }
}

private const val DEFAULT_ANIMATION_DURATION_MILLISECONDS = 350f
private const val DEFAULT_STACK_SPACING = 8f
