//
//  PageSheetView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.foundation.constants.PageSheetViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.constants.SystemAnimationFloats
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import kotlin.math.roundToInt

// MARK: - Constants Accessors

private typealias SheetFloats = PageSheetViewFloats

/**
 * A modal page sheet that slides up over the current content, presented
 * while [isPresented] is `true`.
 *
 * The sheet presents and dismisses on the sheet spring, over dimmed
 * content – or, while [SystemAnimation.usesAppleAnimations] is `false`,
 * on Material's bottom sheet spring over Material's scrim. It has no drag handle and rests just
 * below the status bar. Swiping the sheet down or tapping the dimmed
 * content dismisses it, unless interactive dismissal is disabled; a
 * scrolling list inside the sheet hands a downward swipe at its top to
 * the sheet. While the sheet presents or dismisses, it ignores swipes
 * and taps on the dimmed content. The system back gesture never
 * dismisses the sheet.
 *
 * When [isPresented] becomes `false`, the sheet animates away before it
 * leaves the composition:
 *
 * ```kotlin
 * PageSheetView(isPresented = isPresented, onDismissRequest = { isPresented = false }) {
 *     ContactSelectorPageView()
 * }
 * ```
 *
 * @param isPresented A Boolean value that indicates whether the sheet
 *   is presented.
 * @param onDismissRequest The action performed once the user swipes the
 *   sheet away or taps the dimmed content, and the sheet finishes
 *   animating away.
 * @param containerColor The color behind the sheet's content. The
 *   default is the grouped content background.
 * @param isInteractiveDismissDisabled A Boolean value that indicates
 *   whether swipes and taps on the dimmed content are ignored, so that
 *   the sheet can only be dismissed programmatically. The default is
 *   `false`.
 * @param ignoresBottomSafeArea A Boolean value that indicates whether
 *   the content extends past the bottom safe area, beneath the
 *   navigation bar and keyboard, rather than ending above it. Content
 *   that ignores it insets itself, such as a list that pads its
 *   content by the bottom safe area. The default is `false`.
 * @param content The content of the sheet.
 */
@Composable
@Suppress("LongParameterList")
fun PageSheetView(
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    containerColor: Color = LocalPantherColors.current.groupedContentBackground,
    isInteractiveDismissDisabled: Boolean = false,
    ignoresBottomSafeArea: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) = PageSheetView(
    item = if (isPresented) Unit else null,
    onDismissRequest = onDismissRequest,
    containerColor = containerColor,
    isInteractiveDismissDisabled = isInteractiveDismissDisabled,
    ignoresBottomSafeArea = ignoresBottomSafeArea,
) { content() }

/**
 * A modal page sheet that slides up over the current content, presented
 * while [item] is non-`null`.
 *
 * The sheet behaves as the `isPresented` variant does. While it animates
 * away, it keeps showing the last non-`null` item:
 *
 * ```kotlin
 * PageSheetView(item = selectedContact, onDismissRequest = { selectedContact = null }) { contact ->
 *     ContactDetailView(contact)
 * }
 * ```
 *
 * @param item The item the sheet presents, or `null` to dismiss it.
 * @param onDismissRequest The action performed once the user swipes the
 *   sheet away or taps the dimmed content, and the sheet finishes
 *   animating away.
 * @param containerColor The color behind the sheet's content. The
 *   default is the grouped content background.
 * @param isInteractiveDismissDisabled A Boolean value that indicates
 *   whether swipes and taps on the dimmed content are ignored, so that
 *   the sheet can only be dismissed programmatically. The default is
 *   `false`.
 * @param ignoresBottomSafeArea A Boolean value that indicates whether
 *   the content extends past the bottom safe area, beneath the
 *   navigation bar and keyboard, rather than ending above it. Content
 *   that ignores it insets itself, such as a list that pads its
 *   content by the bottom safe area. The default is `false`.
 * @param content The content of the sheet for the given item.
 */
@Composable
@Suppress("LongParameterList")
fun <T : Any> PageSheetView(
    item: T?,
    onDismissRequest: () -> Unit,
    containerColor: Color = LocalPantherColors.current.groupedContentBackground,
    isInteractiveDismissDisabled: Boolean = false,
    ignoresBottomSafeArea: Boolean = false,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    // The last presented item keeps the sheet's content in place while
    // the sheet animates away.
    val presentedItem = remember { PresentedItem<T>() }
    if (item != null) presentedItem.value = item
    var hasFinishedDismissing by remember { mutableStateOf(item == null) }
    LaunchedEffect(item != null) { if (item != null) hasFinishedDismissing = false }

    val shownItem = presentedItem.value
    if (shownItem == null || (item == null && hasFinishedDismissing)) return

    val sheetState = remember { PageSheetState() }
    val currentIsPresented by rememberUpdatedState(item != null)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    LaunchedEffect(sheetState) {
        // A swipe or a tap on the dimmed content leaves a presented sheet
        // hidden; report the dismissal once it settles there.
        snapshotFlow { sheetState.draggableState.settledValue }
            .dropWhile { it == SheetValue.HIDDEN }
            .collect { value ->
                if (value == SheetValue.HIDDEN && currentIsPresented) currentOnDismissRequest()
            }
    }

    LaunchedEffect(sheetState.hasAnchors, item != null) {
        if (item != null) {
            if (sheetState.hasAnchors) sheetState.present()
            return@LaunchedEffect
        }

        // No gesture can interrupt the dismissal, so the sheet leaves the
        // composition unless it is presented again first.
        if (sheetState.hasAnchors) sheetState.dismiss()
        presentedItem.value = null
        hasFinishedDismissing = true
    }

    PageSheetDialog(
        sheetState = sheetState,
        containerColor = containerColor,
        isInteractive = item != null && !isInteractiveDismissDisabled && !sheetState.isTransitioning,
        ignoresBottomSafeArea = ignoresBottomSafeArea,
    ) { content(shownItem) }
}

// MARK: - Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSheetDialog(
    sheetState: PageSheetState,
    containerColor: Color,
    isInteractive: Boolean,
    ignoresBottomSafeArea: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentIsInteractive by rememberUpdatedState(isInteractive)
    val velocityThreshold = with(LocalDensity.current) { SHEET_VELOCITY_THRESHOLD.toPx() }
    val dimmingAlpha =
        if (SystemAnimation.usesAppleAnimations) {
            SystemAnimationFloats.SHEET_DIMMING_ALPHA
        } else {
            SystemAnimationFloats.MATERIAL_SHEET_SCRIM_ALPHA
        }
    val nestedScrollConnection =
        remember(sheetState) {
            PageSheetNestedScrollConnection(sheetState, velocityThreshold) { currentIsInteractive }
        }
    val flingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            animationSpec = sheetAnimationSpec,
            positionalThreshold = { distance -> distance / 2f },
            state = sheetState.draggableState,
        )

    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                decorFitsSystemWindows = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        DialogWindowEffect(ThemeService.isDarkModeActive(isSystemInDarkTheme()))
        Box(modifier = Modifier.fillMaxSize()) {
            // The dimming follows the sheet as it moves.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(Color.Black, alpha = dimmingAlpha * sheetState.presentedFraction)
                        }.pointerInput(sheetState) {
                            detectTapGestures {
                                if (!currentIsInteractive) return@detectTapGestures
                                scope.launch { sheetState.hide() }
                            }
                        },
            )

            Surface(
                color = containerColor,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .padding(top = SheetFloats.TOP_SPACING.dp)
                        .widthIn(max = BottomSheetDefaults.SheetMaxWidth)
                        .fillMaxWidth()
                        .onSizeChanged { sheetState.updateAnchors(it.height.toFloat()) }
                        .offset { IntOffset(0, sheetState.offset.roundToInt()) }
                        .nestedScroll(nestedScrollConnection)
                        .anchoredDraggable(
                            enabled = isInteractive,
                            flingBehavior = flingBehavior,
                            orientation = Orientation.Vertical,
                            state = sheetState.draggableState,
                        ),
                shape = BottomSheetDefaults.ExpandedShape,
                tonalElevation = BottomSheetDefaults.Elevation,
            ) {
                Column(
                    content = content,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (ignoresBottomSafeArea) {
                                    Modifier
                                } else {
                                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                                },
                            ),
                )
            }
        }
    }
}

// MARK: - Auxiliary

// The fling speed past which a sheet settles in the fling's direction.
private val SHEET_VELOCITY_THRESHOLD = 125.dp
