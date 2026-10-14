//
//  PageSheetView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
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
import androidx.compose.runtime.mutableIntStateOf
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
 * The sheet presents and dismisses on UIKit's sheet spring, over
 * content dimmed as iOS dims it – or, while
 * [SystemAnimation.usesAppleAnimations] is `false`, on Material's bottom
 * sheet spring over Material's scrim. It has no drag handle and rests just
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
    private var transitionCount by mutableIntStateOf(0)

    // MARK: - Computed Properties

    // Whether the sheet is animating on behalf of its presentation or
    // dismissal.
    val isTransitioning: Boolean
        get() = transitionCount > 0

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

    suspend fun dismiss() = transition(to = SheetValue.HIDDEN)

    // Hides the sheet in response to a tap on the dimmed content. A drag
    // interrupts it.
    suspend fun hide() = draggableState.animateTo(SheetValue.HIDDEN, sheetAnimationSpec)

    suspend fun present() = transition(to = SheetValue.PRESENTED)

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
        draggableState.animateTo(target, sheetAnimationSpec)
    }

    // Keeps an animation under way headed for the resting position it
    // was already animating to.
    fun updateAnchors(height: Float) {
        draggableState.updateAnchors(
            DraggableAnchors {
                SheetValue.PRESENTED at 0f
                SheetValue.HIDDEN at height
            },
            draggableState.targetValue,
        )
        hasAnchors = true
    }

    // Animates the sheet to the given resting position on behalf of its
    // presentation or dismissal. The animation outranks any gesture, so
    // a drag or a tap on the dimmed content cannot interrupt it.
    private suspend fun transition(to: SheetValue) {
        transitionCount++
        try {
            draggableState.anchoredDrag(to, MutatePriority.PreventUserInput) { anchors, target ->
                val initialOffset = draggableState.offset
                val targetOffset = anchors.positionOf(target)
                if (initialOffset.isNaN() || targetOffset.isNaN() || initialOffset == targetOffset) return@anchoredDrag
                animate(
                    animationSpec = sheetAnimationSpec,
                    initialValue = initialOffset,
                    initialVelocity = draggableState.lastVelocity,
                    targetValue = targetOffset,
                ) { value, velocity -> dragTo(value, velocity) }
            }
        } finally {
            transitionCount--
        }
    }
}

// Hands scrolling between a sheet and a list inside it: a drag that
// begins with the list at its top lowers the sheet, and an upward swipe
// raises a partly lowered sheet before the list scrolls. A drag that
// begins while the list can still scroll down stays with the list, even
// once the list reaches its top – as with a UIKit scroll view in a sheet.
// While the sheet ignores swipes, the list keeps every drag and fling.
private class PageSheetNestedScrollConnection(
    private val sheetState: PageSheetState,
    private val velocityThreshold: Float,
    private val isEnabled: () -> Boolean,
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
        if (!isEnabled() || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!isEnabled() || source != NestedScrollSource.UserInput) return Offset.Zero
        val isDraggingSheet = isDraggingSheet ?: (consumed.y == 0f && available.y > 0f).also { this.isDraggingSheet = it }
        if (!isDraggingSheet) return Offset.Zero
        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        isDraggingSheet = null
        if (!isEnabled()) return Velocity.Zero
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
        if (!isEnabled()) return Velocity.Zero
        val draggableState = sheetState.draggableState
        if (draggableState.requireOffset() <= draggableState.anchors.minPosition()) return Velocity.Zero
        sheetState.settle(available.y, velocityThreshold)
        return available
    }
}

// The fling speed past which a sheet settles in the fling's direction.
private val SHEET_VELOCITY_THRESHOLD = 125.dp

// The distance, in pixels, from its resting position at which a sheet
// animation ends.
private const val SHEET_VISIBILITY_THRESHOLD = 1f

// The spring of every sheet animation, ending once the sheet visibly
// comes to rest.
private val sheetAnimationSpec: SpringSpec<Float>
    get() = SystemAnimation.sheetTransition(SHEET_VISIBILITY_THRESHOLD)

// An offset that keeps a sheet below the screen before its height is known.
private const val UNMEASURED_OFFSET = 100_000f
