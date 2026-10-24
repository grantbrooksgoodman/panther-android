//
//  HUDHost.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.hud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.constants.HUDHostColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.HUDHostFloats
import us.neotechnica.panther.designsystem.modules.foundation.constants.HUDHostStrings
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias Colors = HUDHostColors
private typealias Floats = HUDHostFloats
private typealias Strings = HUDHostStrings

/**
 * Renders the heads-up display requested through [HUDPresenter].
 *
 * Place a single [HUDHost] near the root of the composition, above the
 * app's content, so the display appears centered over the current
 * screen, or over the space above the keyboard while it is visible. A
 * success or flash display dismisses itself after a short interval
 * scaled to its text; a progress display remains until dismissed and,
 * when modal, blocks interaction with the underlying content.
 */
@Composable
fun HUDHost() {
    val presentation by HUDPresenter.presentation.collectAsState()
    val isPresented = presentation != null

    val opacity = remember { Animatable(0f) }
    val scale = remember { Animatable(Floats.DISPLAY_SCALE) }
    var isDisplayed by remember { mutableStateOf(false) }

    LaunchedEffect(presentation) {
        val current = presentation
        val text =
            when (current) {
                is HUDPresenter.Presentation.Success -> current.text
                is HUDPresenter.Presentation.Flash -> current.text
                else -> return@LaunchedEffect
            }

        delay(Floats.AUTO_DISMISS_PER_CHARACTER_MILLISECONDS * (text?.length ?: 0) + Floats.AUTO_DISMISS_BASE_MILLISECONDS)
        if (HUDPresenter.presentation.value === current) HUDPresenter.hide()
    }

    // A hidden display appears enlarged and transparent, shrinking as
    // it fades in; a dismissed display shrinks further as it fades out.
    LaunchedEffect(isPresented) {
        if (!isPresented) {
            launch { scale.animateTo(Floats.DISMISSAL_SCALE, hudAnimationSpec()) }
            opacity.animateTo(0f, hudAnimationSpec())
            isDisplayed = false
            return@LaunchedEffect
        }

        if (!isDisplayed) {
            opacity.snapTo(0f)
            scale.snapTo(Floats.DISPLAY_SCALE)
            isDisplayed = true
        }

        launch { scale.animateTo(1f / Floats.DISPLAY_SCALE, hudAnimationSpec()) }
        opacity.animateTo(1f, hudAnimationSpec())
    }

    // Retain the last display so the exit animation renders the
    // outgoing content rather than empty space.
    var lastPresentation by remember { mutableStateOf<HUDPresenter.Presentation?>(null) }
    presentation?.let { lastPresentation = it }

    // An image that appears with the display waits for it to settle.
    val isAppearing = remember(presentation) { !isDisplayed }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
    ) {
        if ((presentation as? HUDPresenter.Presentation.Progress)?.isModal == true) {
            ModalBlocker()
        }

        val displayedPresentation = lastPresentation ?: return@Box
        if (!isPresented && !isDisplayed) return@Box
        HUDCard(
            displayedPresentation,
            isAppearing = isAppearing,
            modifier =
                Modifier.graphicsLayer {
                    alpha = opacity.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

// MARK: - Modal Blocker

@Composable
private fun ModalBlocker() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                ) {},
    )
}

// MARK: - Card

// The indicator or image, with the status label below it when the
// display has text.
@Composable
private fun HUDCard(
    presentation: HUDPresenter.Presentation,
    isAppearing: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .clip(RoundedCornerShape(Floats.CORNER_RADIUS.dp))
                .background(if (isDarkModeActive) Colors.DARK_BACKGROUND else Colors.LIGHT_BACKGROUND)
                .padding(
                    start = Floats.MARGIN_SIZE.dp,
                    top = Floats.MARGIN_SIZE.dp,
                    end = Floats.MARGIN_SIZE.dp,
                ),
    ) {
        key(presentation) { Media(presentation, isAppearing) }

        val text = presentation.text?.sanitized
        if (text == null) {
            Spacer(modifier = Modifier.height(Floats.MARGIN_SIZE.dp))
            return@Column
        }

        StatusLabel(
            text,
            foregroundColor = if (isDarkModeActive) Colors.DARK_STATUS_LABEL_FOREGROUND else Colors.LIGHT_STATUS_LABEL_FOREGROUND,
            modifier = Modifier.padding(vertical = (Floats.MARGIN_SIZE / 2).dp),
        )
    }
}

// A centered label that wraps within the maximum width, as wide as
// its widest line.
@Composable
private fun StatusLabel(
    text: String,
    foregroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val font = Font.systemBold(FontScale.Custom(Floats.STATUS_LABEL_FONT_SIZE))
    val textMeasurer = rememberTextMeasurer()

    val width =
        remember(text, density) {
            val layout =
                textMeasurer.measure(
                    text,
                    constraints = Constraints(maxWidth = with(density) { Floats.STATUS_LABEL_MAXIMUM_WIDTH.dp.roundToPx() }),
                    density = Density(density.density),
                    style = font.textStyle,
                )

            (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
        }

    Components.Text(
        text,
        foregroundColor = foregroundColor,
        font = font,
        textAlign = TextAlign.Center,
        modifier = modifier.width(with(density) { ceil(width).toDp() }),
    )
}

// MARK: - Media

@Composable
private fun Media(
    presentation: HUDPresenter.Presentation,
    isAppearing: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(Floats.MEDIA_SIZE.dp),
    ) {
        when (presentation) {
            is HUDPresenter.Presentation.Progress -> ActivityIndicator()

            is HUDPresenter.Presentation.Success -> Checkmark(isAppearing)

            is HUDPresenter.Presentation.Flash ->
                when (presentation.image) {
                    HUD.HUDImage.SUCCESS -> Checkmark(isAppearing)

                    HUD.HUDImage.EXCLAMATION ->
                        Components.Symbol(
                            Strings.EXCLAMATION_IMAGE_SYSTEM_NAME,
                            foregroundColor = Colors.MEDIA_FOREGROUND,
                            modifier =
                                Modifier
                                    .width(Floats.EXCLAMATION_IMAGE_WIDTH.dp)
                                    .fillMaxHeight(),
                        )
                }
        }
    }
}

// Spokes around a center, with a highlight that steps clockwise from
// spoke to spoke, each spoke fading back to rest behind it.
@Composable
private fun ActivityIndicator() {
    val cycleTickCount = Floats.INDICATOR_SPOKE_COUNT * Floats.INDICATOR_TICKS_PER_SPOKE
    val tick =
        rememberInfiniteTransition(label = "activityIndicator").animateValue(
            initialValue = 0,
            targetValue = cycleTickCount,
            typeConverter = Int.VectorConverter,
            animationSpec =
                infiniteRepeatable(
                    tween(
                        durationMillis = cycleTickCount * Floats.INDICATOR_TICK_DURATION_MILLISECONDS,
                        easing = LinearEasing,
                    ),
                ),
            label = "activityIndicatorTick",
        )

    Canvas(modifier = Modifier.size(Floats.MEDIA_SIZE.dp)) {
        val spokeWidth = Floats.INDICATOR_SPOKE_WIDTH.dp.toPx()
        repeat(Floats.INDICATOR_SPOKE_COUNT) { index ->
            rotateRad(radians = (2 * PI * index / Floats.INDICATOR_SPOKE_COUNT).toFloat()) {
                drawLine(
                    color =
                        Colors.MEDIA_FOREGROUND.copy(
                            alpha = spokeOpacity(ticksSinceHighlight = (tick.value - index * Floats.INDICATOR_TICKS_PER_SPOKE).mod(cycleTickCount)),
                        ),
                    start = Offset(center.x, center.y - Floats.INDICATOR_SPOKE_INNER_RADIUS.dp.toPx() - spokeWidth / 2),
                    end = Offset(center.x, center.y - Floats.INDICATOR_SPOKE_OUTER_RADIUS.dp.toPx() + spokeWidth / 2),
                    strokeWidth = spokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

// A checkmark that strokes itself in – once the display settles, if
// it appears with the display.
@Composable
private fun Checkmark(isAppearing: Boolean) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(
                delayMillis = if (isAppearing) Floats.CHECKMARK_APPEARANCE_DELAY_MILLISECONDS else 0,
                durationMillis = Floats.CHECKMARK_STROKE_DURATION_MILLISECONDS,
                easing = LinearEasing,
            ),
        )
    }

    Canvas(modifier = Modifier.size(Floats.MEDIA_SIZE.dp)) {
        if (progress.value == 0f) return@Canvas
        val pathMeasure =
            PathMeasure().apply {
                setPath(
                    Path().apply {
                        moveTo(size.width * Floats.CHECKMARK_START_X, size.width * Floats.CHECKMARK_START_Y)
                        lineTo(size.width * Floats.CHECKMARK_VERTEX_X, size.width * Floats.CHECKMARK_VERTEX_Y)
                        lineTo(size.width * Floats.CHECKMARK_END_X, size.width * Floats.CHECKMARK_END_Y)
                    },
                    forceClosed = false,
                )
            }

        val strokedPath = Path()
        pathMeasure.getSegment(0f, pathMeasure.length * progress.value, strokedPath)

        drawPath(
            strokedPath,
            color = Colors.MEDIA_FOREGROUND,
            style =
                Stroke(
                    width = Floats.CHECKMARK_LINE_WIDTH.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )
    }
}

// MARK: - Auxiliary

// The ease-in with which the display appears and dismisses.
private fun hudAnimationSpec(): FiniteAnimationSpec<Float> = SystemAnimation.easeIn(Floats.ANIMATION_DURATION_MILLISECONDS.milliseconds)

// The opacity of an activity indicator spoke the given number of ticks
// after the highlight reached it.
private fun spokeOpacity(ticksSinceHighlight: Int): Float =
    maxOf(
        Floats.INDICATOR_HIGHLIGHTED_SPOKE_OPACITY -
            (Floats.INDICATOR_HIGHLIGHTED_SPOKE_OPACITY - Floats.INDICATOR_RESTING_SPOKE_OPACITY) *
            ticksSinceHighlight / Floats.INDICATOR_FADE_TICK_COUNT,
        Floats.INDICATOR_RESTING_SPOKE_OPACITY,
    )
