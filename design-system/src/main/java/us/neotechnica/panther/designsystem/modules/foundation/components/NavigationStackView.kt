//
//  NavigationStackView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import us.neotechnica.panther.designsystem.modules.foundation.constants.SystemAnimationFloats
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias Floats = SystemAnimationFloats

/**
 * A navigation stack that pushes and pops its pages as UIKit does.
 *
 * The page on top of [path] is visible. A push slides the incoming
 * page in from the trailing edge over the current page, which slides a
 * fraction of its width toward the leading edge and dims; the incoming
 * page casts a shadow along its leading edge. A pop reverses the
 * motion. Both run on UIKit's navigation spring.
 *
 * Pass [isRootPersistent] to keep [root] composed beneath the stack
 * for its whole lifetime – so that its state survives pushes – rather
 * than composing it only while it is on top. A persistent root still
 * slides and dims beneath a push.
 *
 * @param path The stack's pages, from the bottom; an empty path shows
 *   the root.
 * @param modifier The modifier for this view.
 * @param isRootPersistent Whether the root stays composed while pages
 *   are pushed over it. The default is `false`.
 * @param root The page at the bottom of the stack, given the modifier
 *   it must apply.
 * @param destination The page for an element of [path].
 */
@Composable
fun <T : Any> NavigationStackView(
    path: List<T>,
    modifier: Modifier = Modifier,
    isRootPersistent: Boolean = false,
    root: @Composable (Modifier) -> Unit,
    destination: @Composable (T) -> Unit,
) {
    val colors = LocalPantherColors.current
    val stackTransition = remember { NavigationTransition(path.size) }
    stackTransition.update(path.size)
    val isPush = stackTransition.isPush

    // A persistent root follows the presence of the transparent stand-in
    // that occupies its place in the stack.
    val rootPresence = remember { mutableStateOf<State<Float>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isRootPersistent) {
            root(
                Modifier
                    .fillMaxSize()
                    .navigationTransition(
                        presence = { rootPresence.value?.value ?: 1f },
                        isMovingPage = false,
                        isPush = isPush,
                    ),
            )
        }

        AnimatedContent(
            contentKey = { it?.let { page -> page::class } },
            label = ANIMATION_LABEL,
            modifier = Modifier.fillMaxSize(),
            targetState = path.lastOrNull(),
            // The pages move themselves (below), so the transition only orders
            // them: the moving page is drawn over the page beneath it.
            transitionSpec = {
                (EnterTransition.None togetherWith ExitTransition.None)
                    .apply { targetContentZIndex = stackTransition.zIndex }
                    .using(SizeTransform(clip = false))
            },
        ) { page ->
            val presence =
                transition.animateFloat(
                    label = PRESENCE_ANIMATION_LABEL,
                    transitionSpec = { SystemAnimation.navigationTransition() },
                ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
            val isEntering = transition.targetState == EnterExitState.Visible

            if (page == null && isRootPersistent) {
                SideEffect { rootPresence.value = presence }
                Box(modifier = Modifier.fillMaxSize())
                return@AnimatedContent
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .navigationTransition(
                            presence = { presence.value },
                            isMovingPage = isPush == isEntering,
                            isPush = isPush,
                        ).background(colors.background),
            ) {
                if (page == null) root(Modifier.fillMaxSize()) else destination(page)
            }
        }
    }
}

// MARK: - Auxiliary

// The direction and drawing order of the stack's most recent transition.
// Both hold steady until the stack's depth next changes, so recomposition
// mid-transition never flips a page's role.
private class NavigationTransition(
    private var depth: Int,
) {
    // MARK: - Properties

    var isPush = true
        private set
    var zIndex = 0f
        private set

    // MARK: - Methods

    fun update(newDepth: Int) {
        if (newDepth == depth) return
        isPush = newDepth > depth
        zIndex += if (isPush) 1f else -1f
        depth = newDepth
    }
}

// Moves and decorates a page through a push or pop, given how present it
// is: `1` while it is the settled top page, `0` once it is covered or
// gone. The moving page – the incoming page of a push or the outgoing
// page of a pop – slides its full width and casts a fading shadow along
// its leading edge; the page beneath it slides a fraction of its width
// and dims.
private fun Modifier.navigationTransition(
    presence: () -> Float,
    isMovingPage: Boolean,
    isPush: Boolean,
): Modifier =
    graphicsLayer {
        val hiddenFraction = 1f - presence()
        translationX =
            if (isMovingPage) {
                size.width * hiddenFraction
            } else {
                -Floats.NAVIGATION_PARALLAX_FRACTION * size.width * hiddenFraction
            }
    }.drawWithContent {
        drawContent()
        val hiddenFraction = 1f - presence()
        if (!isMovingPage) {
            if (hiddenFraction > 0f) drawRect(Color.Black, alpha = Floats.NAVIGATION_DIMMING_ALPHA * hiddenFraction)
            return@drawWithContent
        }

        // The shadow fades out over the course of the transition.
        val shadowAlpha = if (isPush) hiddenFraction else 1f - hiddenFraction
        if (shadowAlpha <= 0f) return@drawWithContent
        val shadowWidth = Floats.navigationEdgeShadowWidth.toPx()
        drawRect(
            alpha = shadowAlpha,
            brush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color.Black.copy(alpha = Floats.NAVIGATION_EDGE_SHADOW_OUTER_ALPHA),
                            Color.Black.copy(alpha = Floats.NAVIGATION_EDGE_SHADOW_INNER_ALPHA),
                        ),
                    endX = 0f,
                    startX = -shadowWidth,
                ),
            size = Size(shadowWidth, size.height),
            topLeft = Offset(-shadowWidth, 0f),
        )
    }

private const val ANIMATION_LABEL = "NavigationStackView"
private const val PRESENCE_ANIMATION_LABEL = "NavigationStackViewPresence"
