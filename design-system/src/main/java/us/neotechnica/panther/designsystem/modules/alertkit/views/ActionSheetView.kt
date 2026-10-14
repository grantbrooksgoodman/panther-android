//
//  ActionSheetView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import us.neotechnica.panther.designsystem.modules.alertkit.components.ActionButtonView
import us.neotechnica.panther.designsystem.modules.alertkit.components.AlertKitDialog
import us.neotechnica.panther.designsystem.modules.alertkit.components.AlertLabelView
import us.neotechnica.panther.designsystem.modules.alertkit.constants.ActionSheetViewFloats
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.separator
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.styled
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitDialogState
import us.neotechnica.panther.designsystem.modules.alertkit.models.AlertKitPalette
import us.neotechnica.panther.designsystem.modules.alertkit.services.AlertPresenter
import us.neotechnica.panther.designsystem.modules.alertkit.services.PresentedAlert
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation

// MARK: - Constants Accessors

private typealias SheetFloats = ActionSheetViewFloats

/**
 * An action sheet: a rounded group holding the optional title and
 * message above one row per action, with the cancel button in its
 * own rounded group below. Destructive actions are red, and
 * preferred actions and the cancel button are semibold.
 *
 * The sheet slides up from the bottom of the screen over dimmed
 * content. Selecting an action, tapping the dimmed content, or
 * pressing back slides the sheet away, then reports the result.
 */
@Composable
internal fun ActionSheetView(alert: PresentedAlert.ActionSheet) {
    val enabledOverrides by AlertPresenter.actionEnabledOverrides.collectAsState()
    val state = remember { AlertKitDialogState() }

    AlertKitDialog(
        state = state,
        onBackPressed = { state.dismiss(alert.onCancel) },
        onDimmingViewTapped = { state.dismiss(alert.onCancel) },
    ) { transition ->
        transition.AnimatedVisibility(
            enter = slideInVertically(SystemAnimation.alertPresentation(IntOffset.VisibilityThreshold)) { it },
            exit = slideOutVertically(SystemAnimation.alertDismissal(IntOffset.VisibilityThreshold)) { it },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = SheetFloats.MARGIN.dp),
            visible = { it },
        ) {
            SheetContent(
                alert = alert,
                enabledOverrides = enabledOverrides,
                onActionSelected = { index -> state.dismiss { alert.onSelect(index) } },
                onCancel = { state.dismiss(alert.onCancel) },
            )
        }
    }
}

// MARK: - Sheet Content

// The action group above the separate cancel button, inset from the
// screen's edges and raised above the navigation bar.
@Composable
private fun SheetContent(
    alert: PresentedAlert.ActionSheet,
    enabledOverrides: Map<Int, Boolean>,
    onActionSelected: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val cancelAction = remember(alert) { Action(alert.cancelButtonTitle, style = ActionStyle.CANCEL) {} }
    val hasHeader = alert.title != null || alert.message != null
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val palette = AlertKitPalette.current
    val shape = RoundedCornerShape(SheetFloats.CORNER_RADIUS.dp)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SheetFloats.MARGIN.dp)
                .padding(bottom = max(navigationBarInset, SheetFloats.MARGIN.dp)),
    ) {
        if (hasHeader || alert.actions.isNotEmpty()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .clip(shape)
                        .background(palette.background)
                        .verticalScroll(rememberScrollState()),
            ) {
                if (hasHeader) Header(alert, palette)
                alert.actions.forEachIndexed { index, action ->
                    ActionButtonView(
                        action = action,
                        font = actionLabelFont(action.style.isPreferred),
                        isEnabled = enabledOverrides[index] ?: action.isEnabled,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(SheetFloats.ACTION_ROW_HEIGHT.dp)
                                .then(if (hasHeader || index > 0) Modifier.separator(palette.separator) else Modifier),
                    ) { onActionSelected(index) }
                }
            }

            Spacer(modifier = Modifier.height(SheetFloats.MARGIN.dp))
        }

        ActionButtonView(
            action = cancelAction,
            font = actionLabelFont(isSemibold = true),
            isEnabled = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SheetFloats.ACTION_ROW_HEIGHT.dp)
                    .clip(shape)
                    .background(palette.cancelButtonBackground),
            onClick = onCancel,
        )
    }
}

// The centered title and message above the actions. The title is
// semibold and the message regular, both in a muted color.
@Composable
private fun Header(
    alert: PresentedAlert.ActionSheet,
    palette: AlertKitPalette,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SheetFloats.HEADER_MINIMUM_HEIGHT.dp)
                .padding(
                    horizontal = SheetFloats.HEADER_LABEL_HORIZONTAL_PADDING.dp,
                    vertical = SheetFloats.HEADER_LABEL_VERTICAL_PADDING.dp,
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                SheetFloats.HEADER_LABEL_SPACING.dp,
                Alignment.CenterVertically,
            ),
    ) {
        alert.title?.let {
            AlertLabelView(
                color = palette.secondaryLabel,
                style = Font.systemSemibold(FontScale.Custom(SheetFloats.HEADER_LABEL_FONT_SIZE)).textStyle,
                text = it.styled(alert.titleAttributes),
            )
        }

        alert.message?.let {
            AlertLabelView(
                color = palette.secondaryLabel,
                style = Font.system(FontScale.Custom(SheetFloats.HEADER_LABEL_FONT_SIZE)).textStyle,
                text = it.styled(alert.messageAttributes),
            )
        }
    }
}

// MARK: - Auxiliary

private fun actionLabelFont(isSemibold: Boolean): Font {
    val scale = FontScale.Custom(SheetFloats.ACTION_LABEL_FONT_SIZE)
    return if (isSemibold) Font.systemSemibold(scale) else Font.system(scale)
}
