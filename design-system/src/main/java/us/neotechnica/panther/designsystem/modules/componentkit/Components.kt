//
//  Components.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.componentkit.models.TextFit
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import androidx.compose.material3.Text as Material3Text

/**
 * Factory functions for creating standard text, button, and symbol
 * components with consistent styling.
 *
 * Use [Components] to compose the app's primitive UI elements:
 *
 * ```kotlin
 * Components.Text(
 *     "Hello, world!",
 *     font = Font.systemBold(FontScale.Large),
 *     foregroundColor = LocalPantherColors.current.titleText,
 * )
 * ```
 */
object Components {
    // MARK: - Text

    /**
     * Displays a styled string.
     *
     * @param text The string to display.
     * @param foregroundColor The color of the text.
     * @param font The font to apply. Defaults to [Font.system].
     * @param modifier The modifier for this component.
     * @param textAlign The horizontal alignment of the text, or `null`
     *   to use the default.
     */
    @Composable
    fun Text(
        text: String,
        foregroundColor: Color,
        font: Font = Font.system,
        modifier: Modifier = Modifier,
        textAlign: TextAlign? = null,
    ) {
        FittedText(
            text = text,
            foregroundColor = foregroundColor,
            fit = TextFit(textAlign = textAlign),
            font = font,
            modifier = modifier,
        )
    }

    // MARK: - Fitted Text

    /**
     * Displays a styled string that shrinks, wraps, and truncates to
     * fit the space it is given, per its [fit].
     *
     * @param text The string to display.
     * @param foregroundColor The color of the text.
     * @param fit How the text fits its bounds — its line, shrink,
     *   overflow, and alignment behavior.
     * @param font The font to apply. Defaults to [Font.system].
     * @param modifier The modifier for this component.
     */
    @Composable
    fun FittedText(
        text: String,
        foregroundColor: Color,
        fit: TextFit,
        font: Font = Font.system,
        modifier: Modifier = Modifier,
    ) {
        // Text sizes are pinned: rendering with a unit font scale
        // keeps a point size constant regardless of the user's
        // font-size setting.
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f),
        ) {
            Material3Text(
                text = text,
                color = foregroundColor,
                autoSize =
                    if (fit.minimumScaleFactor < 1f) {
                        TextAutoSize.StepBased(
                            minFontSize = (font.scale.points * fit.minimumScaleFactor).sp,
                            maxFontSize = font.scale.points.sp,
                        )
                    } else {
                        null
                    },
                maxLines = fit.maxLines,
                modifier = modifier,
                overflow = fit.overflow,
                style = font.textStyle,
                textAlign = fit.textAlign,
            )
        }
    }

    // MARK: - Button

    /**
     * A button that displays a styled text label.
     *
     * @param text The label text.
     * @param foregroundColor The color of the label.
     * @param onClick The action to perform when tapped.
     * @param font The font to apply to the label. Defaults to
     *   [Font.system].
     * @param modifier The modifier for this component.
     */
    @Composable
    fun Button(
        text: String,
        foregroundColor: Color,
        onClick: () -> Unit,
        font: Font = Font.system,
        modifier: Modifier = Modifier,
    ) {
        Box(modifier = modifier.clickable(onClick = onClick)) {
            Text(
                text,
                foregroundColor = foregroundColor,
                font = font,
            )
        }
    }

    /**
     * A button that displays a symbol label.
     *
     * @param symbolName The SF Symbol name (mapped to a Material symbol).
     * @param foregroundColor The tint of the symbol.
     * @param onClick The action to perform when tapped.
     * @param modifier The modifier for this component.
     */
    @Composable
    fun Button(
        symbolName: String,
        foregroundColor: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Box(modifier = modifier.clickable(onClick = onClick)) {
            Symbol(
                symbolName,
                foregroundColor = foregroundColor,
            )
        }
    }

    // MARK: - Capsule Button

    /**
     * A prominent, filled capsule button with themed defaults.
     *
     * When disabled, it uses the theme's disabled fill and ignores
     * taps.
     *
     * @param text The label text.
     * @param backgroundColor The capsule's fill color. The default
     *   is the theme's accent color.
     * @param font The font to apply to the label.
     * @param foregroundColor The color of the label. The default is
     *   the theme's background color.
     * @param usesShadow Whether the capsule casts a shadow. The
     *   default is `true`.
     * @param isEnabled Whether the button responds to taps.
     * @param modifier The modifier for this component.
     * @param action The action to perform when tapped.
     */
    @Composable
    @Suppress("LongParameterList")
    fun CapsuleButton(
        text: String,
        backgroundColor: Color = LocalPantherColors.current.accent,
        font: Font = Font.systemSemibold(),
        foregroundColor: Color = LocalPantherColors.current.background,
        usesShadow: Boolean = true,
        isEnabled: Boolean = true,
        modifier: Modifier = Modifier,
        action: () -> Unit,
    ) {
        val colors = LocalPantherColors.current
        val fillColor = if (isEnabled) backgroundColor else colors.disabled
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                modifier
                    .then(
                        if (usesShadow) {
                            Modifier.shadow(CAPSULE_SHADOW_ELEVATION, RoundedCornerShape(CAPSULE_CORNER_RADIUS))
                        } else {
                            Modifier
                        },
                    ).clip(RoundedCornerShape(CAPSULE_CORNER_RADIUS))
                    .background(fillColor)
                    .clickable(enabled = isEnabled, onClick = action)
                    .padding(
                        horizontal = CAPSULE_HORIZONTAL_PADDING,
                        vertical = CAPSULE_VERTICAL_PADDING,
                    ),
        ) {
            Text(text, foregroundColor = foregroundColor, font = font)
        }
    }

    // MARK: - Symbol

    /**
     * Displays a symbol image for an SF Symbol name.
     *
     * @param systemName The SF Symbol name. Unmapped names render a
     *   warning symbol.
     * @param foregroundColor The tint of the symbol.
     * @param modifier The modifier for this component.
     * @param weight The stroke weight to request. Accepted for
     *   signature compatibility; vector symbols carry no weight
     *   axis.
     * @param usesIntrinsicSize Whether the symbol renders at its
     *   intrinsic size rather than scaling to its bounds. The
     *   default is `true`.
     */
    @Suppress("UnusedParameter")
    @Composable
    fun Symbol(
        systemName: String,
        foregroundColor: Color,
        modifier: Modifier = Modifier,
        weight: FontWeight? = null,
        usesIntrinsicSize: Boolean = true,
    ) {
        Icon(
            imageVector = SFSymbol.imageVector(systemName),
            contentDescription = null,
            modifier = modifier,
            tint = foregroundColor,
        )
    }
}

private val CAPSULE_CORNER_RADIUS = 24.dp
private val CAPSULE_HORIZONTAL_PADDING = 18.dp
private val CAPSULE_VERTICAL_PADDING = 8.dp
private val CAPSULE_SHADOW_ELEVATION = 6.dp
