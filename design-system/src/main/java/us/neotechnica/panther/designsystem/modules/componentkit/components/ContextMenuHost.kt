//
//  ContextMenuHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import us.neotechnica.panther.designsystem.R
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.GestureRecognizerState
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.foundation.services.SoundPlayer

/**
 * A presented context menu: the lifted message bubble and its action
 * menu.
 *
 * The anchor bounds are in the host's coordinate space, where the
 * overlay draws the lifted bubble, so a transform the host shares with
 * the bubble – the page sliding in as it is pushed – can't move the
 * lifted bubble away from its origin.
 */
internal data class ActiveContextMenu(
    val originID: Any,
    val anchorBounds: Rect,
    val alignment: ContextMenuAlignment,
    val actions: List<ContextMenuAction>,
    val reactionChoices: List<ReactionChoice>,
    val liftScale: Float,
    val liftedBackground: Color?,
    val menuLeadingOffset: Dp,
    val alignsMenuCardToLeadingEdge: Boolean,
    val content: @Composable () -> Unit,
)

/**
 * Controls presentation of a message context menu within a
 * [ContextMenuHost].
 *
 * A long press that presents a menu carries into it: dragging the
 * held pointer over the menu's elements highlights the one beneath
 * it, and lifting the pointer over an element selects it.
 */
class ContextMenuController internal constructor() {
    // MARK: - Properties

    internal var active by mutableStateOf<ActiveContextMenu?>(null)
        private set

    // The last presented menu, kept while it animates away after dismissal.
    internal var lastPresented by mutableStateOf<ActiveContextMenu?>(null)

    internal var canBegin: Boolean = true
    internal var hapticFeedback: HapticFeedback? = null

    // The host's layout coordinates, against which lifted bubbles are
    // measured.
    internal var hostCoordinates: LayoutCoordinates? = null

    /** The index of the menu element under a held long press, or `null`. */
    internal var highlightedElementIndex by mutableStateOf<Int?>(null)
        private set

    private val elementCoordinates = mutableMapOf<Int, LayoutCoordinates>()
    private val registry = mutableMapOf<String, () -> ActiveContextMenu?>()

    // The location, in the root coordinate space, at which the held long
    // press that presented the current menu began.
    private var interactionOrigin: Offset? = null

    // MARK: - Computed Properties

    /** Whether a context menu is currently presented. */
    val isPresenting: Boolean
        get() = active != null

    // The menu on screen: the presented menu, or the dismissed menu while
    // it animates away.
    internal val displayed: ActiveContextMenu?
        get() = active ?: lastPresented

    // MARK: - Host Bounds

    /**
     * Returns the bounds of the given layout in the host's coordinate
     * space, or `null` when either is detached.
     *
     * @param coordinates The layout coordinates of a bubble within the
     *   host.
     */
    internal fun boundsInHost(coordinates: LayoutCoordinates): Rect? {
        val hostCoordinates = hostCoordinates?.takeIf { it.isAttached } ?: return null
        if (!coordinates.isAttached) return null
        return Rect(hostCoordinates.localPositionOf(coordinates, Offset.Zero), coordinates.size.toSize())
    }

    // MARK: - Presentation

    internal fun present(item: ActiveContextMenu) {
        resetElements()
        lastPresented = item
        active = item
    }

    /**
     * Presents the menu registered under the given key, if one is
     * registered and can currently be built.
     *
     * @param key The key of the menu to present.
     *
     * @return `true` if a menu was presented; otherwise, `false`.
     */
    fun present(key: String): Boolean {
        val item = registry[key]?.invoke() ?: return false
        present(item)
        return true
    }

    internal fun register(
        key: String,
        factory: () -> ActiveContextMenu?,
    ) {
        registry[key] = factory
    }

    internal fun unregister(key: String) {
        registry.remove(key)
    }

    // Dismisses the presented menu and performs the given selection – an
    // action or a reaction – unless the menu is already animating away.
    internal fun select(selection: () -> Unit) {
        if (!isPresenting) return
        dismiss()
        selection()
    }

    /** Dismisses the presented context menu, if any. */
    fun dismiss() {
        interactionOrigin = null
        resetElements()
        active = null
    }

    // MARK: - Interaction

    /**
     * Handles a change in the state of a long press on a message
     * bubble.
     *
     * When the press begins, the bubble's menu is presented. As the
     * held pointer moves, the menu element beneath it is highlighted.
     * When the pointer lifts over an element, having travelled more
     * than [dragThreshold] from where the press began, the menu
     * dismisses and the element's action runs; lifting anywhere else
     * leaves the menu presented.
     *
     * @param state The long press's new state.
     * @param location The pointer's location in the root coordinate
     *   space.
     * @param dragThreshold The distance, in pixels, the pointer must
     *   travel from where the press began for its release to select
     *   an element.
     * @param menu Returns the menu to present as the press begins, or
     *   `null` when the bubble has none.
     */
    internal fun beginInteraction(
        state: GestureRecognizerState,
        location: Offset,
        dragThreshold: Float,
        menu: () -> ActiveContextMenu?,
    ) {
        when (state) {
            GestureRecognizerState.BEGAN -> {
                if (!canBegin || active != null) return
                val item = menu() ?: return
                SoundPlayer.play(R.raw.selection)
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
                present(item)
                interactionOrigin = location
            }

            GestureRecognizerState.CHANGED -> if (interactionOrigin != null) highlightElement(location)

            GestureRecognizerState.ENDED -> {
                val origin = interactionOrigin ?: return
                interactionOrigin = null
                val didDragFromOrigin = (location - origin).getDistance() > dragThreshold
                val element = if (didDragFromOrigin) element(location) else null
                unhighlightAllElements()

                element?.let { select(it.onSelect) }
            }

            GestureRecognizerState.CANCELLED -> {
                interactionOrigin = null
                unhighlightAllElements()
            }
        }
    }

    // MARK: - Menu Elements

    /**
     * Records the layout coordinates of the presented menu's element at
     * the given index, for hit-testing a held long press.
     *
     * @param index The element's index among the menu's actions.
     * @param coordinates The element's layout coordinates.
     */
    internal fun setElementCoordinates(
        index: Int,
        coordinates: LayoutCoordinates,
    ) {
        elementCoordinates[index] = coordinates
    }

    // Returns the presented menu's action whose element contains the
    // given location, or `null`.
    private fun element(location: Offset): ContextMenuAction? = elementIndex(location)?.let { active?.actions?.getOrNull(it) }

    private fun elementIndex(location: Offset): Int? =
        elementCoordinates.entries
            .firstOrNull { (_, coordinates) -> coordinates.isAttached && coordinates.boundsInRoot().contains(location) }
            ?.key

    // Highlights the element at the given location, if any, playing a
    // selection haptic as the highlight moves from one element directly
    // to another.
    private fun highlightElement(location: Offset) {
        val newHighlightedElementIndex = elementIndex(location)
        if (newHighlightedElementIndex == highlightedElementIndex) return

        if (highlightedElementIndex != null && newHighlightedElementIndex != null) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

        highlightedElementIndex = newHighlightedElementIndex
    }

    private fun resetElements() {
        elementCoordinates.clear()
        unhighlightAllElements()
    }

    private fun unhighlightAllElements() {
        highlightedElementIndex = null
    }
}

/** Provides the [ContextMenuController] to descendant [MessageContextMenu]s. */
val LocalContextMenuController = compositionLocalOf<ContextMenuController?> { null }

/**
 * Hosts message context menus in the same composition as [content], so
 * the lifted bubble aligns exactly with its origin over a dimmed,
 * tap-to-dismiss backdrop.
 *
 * @param modifier The modifier for the host.
 * @param canBegin Whether a long press may present a menu; when
 *   `false`, long presses are ignored but double-taps still fire.
 * @param content The hosted content, containing [MessageContextMenu]s.
 */
@Composable
fun ContextMenuHost(
    modifier: Modifier = Modifier,
    canBegin: Boolean = true,
    content: @Composable () -> Unit,
) {
    val controller = remember { ContextMenuController() }
    controller.canBegin = canBegin
    controller.hapticFeedback = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }

    // The menu appears and disappears on ContextualMenu's spring. UIKit's
    // initial spring velocity is relative to the distance left to travel,
    // so a dismissal starts out heading back toward rest. Without iOS
    // animations, the menu moves on Compose's default spring from rest.
    LaunchedEffect(controller.active) {
        val usesAppleAnimations = SystemAnimation.usesAppleAnimations
        val stiffness = if (usesAppleAnimations) PRESENTATION_SPRING_STIFFNESS else Spring.StiffnessMediumLow
        val initialVelocity = if (usesAppleAnimations) PRESENTATION_SPRING_INITIAL_VELOCITY else 0f
        if (controller.active != null) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness),
                initialVelocity = initialVelocity,
            )
            return@LaunchedEffect
        }

        if (controller.lastPresented == null) return@LaunchedEffect
        progress.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness),
            initialVelocity = -initialVelocity * progress.value,
        )
        controller.lastPresented = null
    }

    // Modifier.blur is a no-op below API 31, so the pre-31 path captures
    // the host into a graphics layer and draws a software-blurred copy
    // behind the overlay instead.
    val usesSoftwareBlur = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    val hostLayer = rememberGraphicsLayer()
    var softwareBlurredBackdrop by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(controller.lastPresented) {
        softwareBlurredBackdrop =
            if (usesSoftwareBlur && controller.lastPresented != null) {
                softwareBlurred(hostLayer.toImageBitmap())
            } else {
                null
            }
    }

    Box(modifier.fillMaxSize().onPlaced { controller.hostCoordinates = it }) {
        Box(
            if (usesSoftwareBlur) {
                Modifier.drawWithContent {
                    hostLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(hostLayer)
                }
            } else {
                Modifier.blur(BLUR_RADIUS * progress.value)
            },
        ) {
            CompositionLocalProvider(LocalContextMenuController provides controller) {
                content()
            }
        }

        controller.displayed?.let { active ->
            softwareBlurredBackdrop?.let { backdrop ->
                Image(
                    bitmap = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ContextMenuOverlay(active = active, progress = progress.value, controller = controller)
        }
    }
}

// Approximates a blur by downscaling the capture and re-expanding it
// with bilinear filtering.
private fun softwareBlurred(image: ImageBitmap): ImageBitmap {
    val bitmap = image.asAndroidBitmap()
    val scaledWidth = (bitmap.width / SOFTWARE_BLUR_DOWNSCALE_FACTOR).coerceAtLeast(1)
    val scaledHeight = (bitmap.height / SOFTWARE_BLUR_DOWNSCALE_FACTOR).coerceAtLeast(1)
    val downscaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    return Bitmap.createScaledBitmap(downscaled, bitmap.width, bitmap.height, true).asImageBitmap()
}

private const val PRESENTATION_SPRING_INITIAL_VELOCITY = 4f

// The stiffness UIKit gives the vendored ContextualMenu's spring: a 0.3-
// second UIView spring animation with a damping ratio of 1.
private const val PRESENTATION_SPRING_STIFFNESS = 918.701f
private const val SOFTWARE_BLUR_DOWNSCALE_FACTOR = 16
