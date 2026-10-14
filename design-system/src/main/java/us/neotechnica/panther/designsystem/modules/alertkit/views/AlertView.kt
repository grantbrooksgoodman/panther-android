//
//  AlertView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.alertkit.components.ActionButtonView
import us.neotechnica.panther.designsystem.modules.alertkit.components.AlertKitDialog
import us.neotechnica.panther.designsystem.modules.alertkit.components.AlertLabelView
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertViewFloats
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.separator
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitDialogState
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitPalette
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation

// MARK: - Constants Accessors

private typealias AlertFloats = AlertViewFloats

/**
 * An alert: a centered, rounded panel with a semibold title and a
 * regular message above an optional accessory, such as a text field,
 * and its action buttons. An alert without a title shows its message
 * in the title's style.
 *
 * Two actions sit side by side, with the cancel action on the
 * leading side, as long as both titles fit; otherwise, the actions
 * stack vertically with the cancel action at the bottom. The
 * preferred action is semibold – or, without one, the cancel action.
 * Destructive actions are red.
 *
 * The alert scales and fades in over dimmed content. Selecting an
 * action fades the alert out, then reports the action's index in
 * the given actions. The accessory spans the alert's width and insets
 * its own content; it receives a closure that selects the preferred
 * action, if one exists and is enabled.
 */
@Composable
internal fun AlertView(
    title: AnnotatedString?,
    message: AnnotatedString?,
    actions: List<Action>,
    accessory: (@Composable (onSubmit: () -> Unit) -> Unit)? = null,
    onSelect: (Int) -> Unit,
) {
    val palette = AlertKitPalette.current
    val state = remember { AlertKitDialogState() }

    val select: (Int) -> Unit = { index -> state.dismiss { onSelect(index) } }
    val submit: () -> Unit = {
        val index = actions.indexOfFirst { it.style.isPreferred }
        val isEnabled = AlertPresenter.actionEnabledOverrides.value[index] ?: actions.getOrNull(index)?.isEnabled
        if (isEnabled == true) select(index)
    }

    AlertKitDialog(state = state) { transition ->
        transition.AnimatedVisibility(
            enter =
                fadeIn(SystemAnimation.alertPresentation()) +
                    scaleIn(
                        SystemAnimation.alertPresentation(),
                        initialScale = AlertFloats.PRESENTATION_INITIAL_SCALE,
                    ),
            exit = fadeOut(SystemAnimation.alertDismissal()),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            visible = { it },
        ) {
            Column(
                modifier =
                    Modifier
                        .width(AlertFloats.WIDTH.dp)
                        .clip(RoundedCornerShape(AlertFloats.CORNER_RADIUS.dp))
                        .background(palette.background),
            ) {
                if (title != null || message != null) {
                    Content(
                        title = title ?: message,
                        message = message.takeIf { title != null },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }

                accessory?.invoke(submit)
                ActionButtons(actions, onSelect = select)
            }
        }
    }
}

// MARK: - Content

// The centered title and message, positioned by their baselines and
// scrolling when they exceed the available height.
@Composable
private fun Content(
    title: AnnotatedString?,
    message: AnnotatedString?,
    modifier: Modifier,
) {
    val palette = AlertKitPalette.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AlertFloats.CONTENT_HORIZONTAL_PADDING.dp),
    ) {
        title?.let {
            AlertLabelView(
                color = palette.label,
                modifier =
                    Modifier.paddingFromBaseline(
                        bottom =
                            if (message == null) {
                                AlertFloats.CONTENT_LAST_BASELINE_TO_BOTTOM.dp
                            } else {
                                AlertFloats.TITLE_LAST_BASELINE_TO_BOTTOM.dp
                            },
                        top = AlertFloats.CONTENT_TOP_TO_FIRST_BASELINE.dp,
                    ),
                style =
                    Font
                        .systemSemibold(FontScale.Custom(AlertFloats.TITLE_LABEL_FONT_SIZE))
                        .textStyle
                        .copy(lineHeight = AlertFloats.TITLE_LABEL_LINE_HEIGHT.sp),
                text = it,
            )
        }

        // The message always follows a title; a title-less alert shows
        // its message as the title.
        message?.let {
            AlertLabelView(
                color = palette.label,
                modifier =
                    Modifier.paddingFromBaseline(
                        bottom = AlertFloats.CONTENT_LAST_BASELINE_TO_BOTTOM.dp,
                        top = AlertFloats.MESSAGE_TOP_TO_FIRST_BASELINE.dp,
                    ),
                style =
                    Font
                        .system(FontScale.Custom(AlertFloats.MESSAGE_LABEL_FONT_SIZE))
                        .textStyle
                        .copy(lineHeight = AlertFloats.MESSAGE_LABEL_LINE_HEIGHT.sp),
                text = it,
            )
        }
    }
}

// The action buttons below the content, side by side or stacked.
@Composable
private fun ActionButtons(
    actions: List<Action>,
    onSelect: (Int) -> Unit,
) {
    val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
    val hasPreferredAction = actions.any { it.style.isPreferred }
    val maximumLabelWidth = with(LocalDensity.current) { AlertFloats.HORIZONTAL_ACTION_LABEL_MAXIMUM_WIDTH.dp.toPx() }
    val palette = AlertKitPalette.current
    val textMeasurer = rememberTextMeasurer()

    // Two actions sit side by side only if both titles fit on a button
    // half the alert's width.
    val isHorizontal =
        actions.size == 2 &&
            actions.all { action ->
                val labelWidth =
                    textMeasurer
                        .measure(
                            action.title.sanitized,
                            maxLines = 1,
                            style = actionLabelFont(action, hasPreferredAction).textStyle,
                        ).size.width

                labelWidth <= maximumLabelWidth
            }

    // Side by side, the cancel action leads; stacked, it trails.
    val cancelIndices = actions.indices.filter { actions[it].style == ActionStyle.CANCEL }
    val otherIndices = actions.indices.filter { actions[it].style != ActionStyle.CANCEL }
    val orderedIndices = if (isHorizontal) cancelIndices + otherIndices else otherIndices + cancelIndices

    val button: @Composable (Int, Modifier) -> Unit = { index, modifier ->
        val action = actions[index]
        ActionButtonView(
            action = action,
            font = actionLabelFont(action, hasPreferredAction),
            isEnabled = enabledOverrides[index] ?: action.isEnabled,
            modifier = modifier,
        ) { onSelect(index) }
    }

    if (isHorizontal) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(AlertFloats.ACTION_BUTTON_HEIGHT.dp)
                    .separator(palette.separator),
        ) {
            orderedIndices.forEachIndexed { position, index ->
                button(
                    index,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (position > 0) Modifier.separator(palette.separator, isLeading = true) else Modifier),
                )
            }
        }
    } else {
        Column {
            orderedIndices.forEach { index ->
                button(
                    index,
                    Modifier
                        .fillMaxWidth()
                        .height(AlertFloats.ACTION_BUTTON_HEIGHT.dp)
                        .separator(palette.separator),
                )
            }
        }
    }
}

// MARK: - Auxiliary

// The preferred action is semibold – or, without one, the cancel
// action.
private fun actionLabelFont(
    action: Action,
    hasPreferredAction: Boolean,
): Font {
    val isSemibold = if (hasPreferredAction) action.style.isPreferred else action.style == ActionStyle.CANCEL
    val scale = FontScale.Custom(AlertFloats.ACTION_LABEL_FONT_SIZE)
    return if (isSemibold) Font.systemSemibold(scale) else Font.system(scale)
}
