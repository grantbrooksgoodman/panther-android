//
//  ContextMenuHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAction
import us.neotechnica.panther.designsystem.modules.componentkit.models.ContextMenuAlignment
import us.neotechnica.panther.designsystem.modules.componentkit.models.ReactionChoice

/**
 * A presented context menu: the lifted message bubble and its action
 * menu.
 */
internal data class ActiveContextMenu(
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

/** Controls presentation of a message context menu within a [ContextMenuHost]. */
class ContextMenuController internal constructor() {
    internal var active by mutableStateOf<ActiveContextMenu?>(null)
        private set

    internal var canBegin: Boolean = true

    private val registry = mutableMapOf<String, () -> ActiveContextMenu?>()

    internal fun present(item: ActiveContextMenu) {
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
        active = item
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

    /** Dismisses the presented context menu, if any. */
    fun dismiss() {
        active = null
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
    val progress = remember { Animatable(0f) }

    LaunchedEffect(controller.active) {
        if (controller.active == null) {
            progress.snapTo(0f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    Box(modifier.fillMaxSize()) {
        // Modifier.blur is a no-op below API 31, so the pre-31 path relies on
        // the dimmed scrim instead of a blur.
        Box(Modifier.blur(BLUR_RADIUS * progress.value)) {
            CompositionLocalProvider(LocalContextMenuController provides controller) {
                content()
            }
        }

        controller.active?.let { active ->
            ContextMenuOverlay(active = active, progress = progress.value, onDismiss = controller::dismiss)
        }
    }
}
