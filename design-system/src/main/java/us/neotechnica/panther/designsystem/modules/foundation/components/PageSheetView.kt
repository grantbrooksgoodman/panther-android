//
//  PageSheetView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.foundation.constants.PageSheetViewFloats
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias SheetFloats = PageSheetViewFloats

/**
 * A modal page sheet that slides up over the current content.
 *
 * The sheet has no drag handle and rests just below the status bar,
 * leaving the dimmed content behind it visible above its top edge.
 * Swiping the sheet down or tapping the dimmed content dismisses it,
 * unless interactive dismissal is disabled. The system back gesture
 * never dismisses the sheet.
 *
 * Add the view to the composition while the sheet is presented, and
 * remove it in response to a dismissal request:
 *
 * ```kotlin
 * if (isPresented) {
 *     PageSheetView(onDismissRequest = { isPresented = false }) {
 *         ContactSelectorPageView()
 *     }
 * }
 * ```
 *
 * @param onDismissRequest The action performed when the user swipes
 *   the sheet down or taps the dimmed content.
 * @param containerColor The color behind the sheet's content. The
 *   default is the grouped content background.
 * @param isInteractiveDismissDisabled A Boolean value that indicates
 *   whether swipes and taps on the dimmed content are ignored, so that
 *   the sheet can only be dismissed programmatically. The default is
 *   `false`.
 * @param content The content of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageSheetView(
    onDismissRequest: () -> Unit,
    containerColor: Color = LocalPantherColors.current.groupedContentBackground,
    isInteractiveDismissDisabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!isInteractiveDismissDisabled) onDismissRequest() },
        // Insetting the sheet's layout area from the top keeps the
        // expanded sheet below the status bar.
        modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = SheetFloats.TOP_SPACING.dp),
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { value ->
                    !(isInteractiveDismissDisabled && value == SheetValue.Hidden)
                },
            ),
        containerColor = containerColor,
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        content = content,
    )
}
