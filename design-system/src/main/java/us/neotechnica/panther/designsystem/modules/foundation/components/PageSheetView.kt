//
//  PageSheetView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * The sheet presents and dismisses on UIKit's sheet spring, over
 * content dimmed as iOS dims it. It has no drag handle and rests just
 * below the status bar. Swiping the sheet down or tapping the dimmed
 * content dismisses it, unless interactive dismissal is disabled; a
 * scrolling list inside the sheet hands a downward swipe at its top to
 * the sheet. The system back gesture never dismisses the sheet.
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
 * @param content The content of the sheet.
 */
@Composable
fun PageSheetView(
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    containerColor: Color = LocalPantherColors.current.groupedContentBackground,
    isInteractiveDismissDisabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) = PageSheetView(
    item = if (isPresented) Unit else null,
    onDismissRequest = onDismissRequest,
    containerColor = containerColor,
    isInteractiveDismissDisabled = isInteractiveDismissDisabled,
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
 * @param content The content of the sheet for the given item.
 */
@Composable
fun <T : Any> PageSheetView(
    item: T?,
    onDismissRequest: () -> Unit,
    containerColor: Color = LocalPantherColors.current.groupedContentBackground,
    isInteractiveDismissDisabled: Boolean = false,
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
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    LaunchedEffect(sheetState) {
        // A swipe or a tap on the dimmed content leaves the sheet hidden;
        // report the dismissal once it settles there.
        snapshotFlow { sheetState.draggableState.settledValue }.collect { value ->
            if (value == SheetValue.PRESENTED) sheetState.hasPresented = true
            if (value == SheetValue.HIDDEN && sheetState.hasPresented && !sheetState.isDismissingProgrammatically) {
                currentOnDismissRequest()
            }
        }
    }

    LaunchedEffect(sheetState.hasAnchors, item != null) {
        if (!sheetState.hasAnchors) {
            if (item == null) hasFinishedDismissing = true
            return@LaunchedEffect
        }

        sheetState.isDismissingProgrammatically = item == null
        if (item != null) return@LaunchedEffect sheetState.present()
        sheetState.dismiss()
        presentedItem.value = null
        hasFinishedDismissing = true
    }

    PageSheetDialog(
        sheetState = sheetState,
        containerColor = containerColor,
        isInteractiveDismissDisabled = isInteractiveDismissDisabled,
    ) { content(shownItem) }
}

// MARK: - Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSheetDialog(
    sheetState: PageSheetState,
    containerColor: Color,
    isInteractiveDismissDisabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentIsInteractiveDismissDisabled by rememberUpdatedState(isInteractiveDismissDisabled)
    val velocityThreshold = with(LocalDensity.current) { SHEET_VELOCITY_THRESHOLD.toPx() }
    val nestedScrollConnection = remember(sheetState) { PageSheetNestedScrollConnection(sheetState, velocityThreshold) }
    val flingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            animationSpec = SystemAnimation.sheetTransition(),
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
                            drawRect(Color.Black, alpha = SystemAnimationFloats.SHEET_DIMMING_ALPHA * sheetState.presentedFraction)
                        }.pointerInput(sheetState) {
                            detectTapGestures {
                                if (currentIsInteractiveDismissDisabled) return@detectTapGestures
                                scope.launch { sheetState.dismiss() }
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
                        .then(
                            if (isInteractiveDismissDisabled) {
                                Modifier
                            } else {
                                Modifier
                                    .nestedScroll(nestedScrollConnection)
                                    .anchoredDraggable(sheetState.draggableState, Orientation.Vertical, flingBehavior = flingBehavior)
                            },
                        ),
                shape = BottomSheetDefaults.ExpandedShape,
                tonalElevation = BottomSheetDefaults.Elevation,
            ) {
                Column(
                    content = content,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                )
            }
        }
    }
}

// MARK: - Auxiliary

// The resting positions of the sheet.
private enum class SheetValue {
    HIDDEN,
    PRESENTED,
}

// Holds the last non-`null` item a sheet presented.
private class PresentedItem<T : Any> {
    var value: T? = null
}

// The position of a sheet, measured as its downward offset from where it
// rests when presented.
private class PageSheetState {
    // MARK: - Properties

    val draggableState = AnchoredDraggableState(SheetValue.HIDDEN)

    var hasAnchors by mutableStateOf(false)
        private set
    var hasPresented = false
    var isDismissingProgrammatically = false

    // MARK: - Computed Properties

    // The sheet's offset, placing it offscreen until its height is known.
    val offset: Float
        get() = draggableState.offset.takeUnless { it.isNaN() } ?: UNMEASURED_OFFSET

    // How far the sheet is presented, from 0 when hidden to 1 when presented.
    val presentedFraction: Float
        get() {
            val hiddenOffset = draggableState.anchors.maxPosition()
            if (!hasAnchors || hiddenOffset <= 0f || draggableState.offset.isNaN()) return 0f
            return (1f - draggableState.offset / hiddenOffset).coerceIn(0f, 1f)
        }

    // MARK: - Methods

    suspend fun dismiss() = draggableState.animateTo(SheetValue.HIDDEN, SystemAnimation.sheetTransition())

    suspend fun present() = draggableState.animateTo(SheetValue.PRESENTED, SystemAnimation.sheetTransition())

    // Settles the sheet after a fling handed off by a scrolling list: a
    // fast enough fling decides the direction; otherwise the sheet returns
    // to the nearer resting position.
    suspend fun settle(
        velocity: Float,
        velocityThreshold: Float,
    ) {
        val target =
            when {
                velocity > velocityThreshold -> SheetValue.HIDDEN
                velocity < -velocityThreshold -> SheetValue.PRESENTED
                else -> draggableState.anchors.closestAnchor(draggableState.requireOffset()) ?: SheetValue.PRESENTED
            }
        draggableState.animateTo(target, SystemAnimation.sheetTransition())
    }

    fun updateAnchors(height: Float) {
        draggableState.updateAnchors(
            DraggableAnchors {
                SheetValue.PRESENTED at 0f
                SheetValue.HIDDEN at height
            },
        )
        hasAnchors = true
    }
}

// Hands scrolling between a sheet and a list inside it: a drag that
// begins with the list at its top lowers the sheet, and an upward swipe
// raises a partly lowered sheet before the list scrolls. A drag that
// begins while the list can still scroll down stays with the list, even
// once the list reaches its top – as with a UIKit scroll view in a sheet.
private class PageSheetNestedScrollConnection(
    private val sheetState: PageSheetState,
    private val velocityThreshold: Float,
) : NestedScrollConnection {
    // MARK: - Properties

    // Whether the current drag moves the sheet: decided by the drag's first
    // movement, or `null` before it.
    private var isDraggingSheet: Boolean? = null

    // MARK: - NestedScrollConnection Conformance

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val isDraggingSheet = isDraggingSheet ?: (consumed.y == 0f && available.y > 0f).also { this.isDraggingSheet = it }
        if (!isDraggingSheet) return Offset.Zero
        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        isDraggingSheet = null
        val draggableState = sheetState.draggableState
        if (available.y >= 0f || draggableState.requireOffset() <= draggableState.anchors.minPosition()) return Velocity.Zero
        sheetState.settle(available.y, velocityThreshold)
        return available
    }

    // A fling the list cannot finish moves the sheet only if a drag has
    // already lowered it – a list flung to its top stops there, as a
    // UIKit scroll view in a sheet does.
    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        val draggableState = sheetState.draggableState
        if (draggableState.requireOffset() <= draggableState.anchors.minPosition()) return Velocity.Zero
        sheetState.settle(available.y, velocityThreshold)
        return available
    }
}

// The fling speed past which a sheet settles in the fling's direction.
private val SHEET_VELOCITY_THRESHOLD = 125.dp

// An offset that keeps a sheet below the screen before its height is known.
private const val UNMEASURED_OFFSET = 100_000f
