//
//  Components.kt
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
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
 *     color = LocalPantherColors.current.titleText,
 * )
 * ```
 */
object Components {
    // MARK: - Text

    /**
     * Displays a styled string.
     *
     * @param text The string to display.
     * @param color The color of the text.
     * @param font The font to apply. Defaults to [Font.system].
     * @param modifier The modifier for this component.
     * @param textAlign The horizontal alignment of the text, or `null`
     *   to use the default.
     */
    @Composable
    fun Text(
        text: String,
        color: Color,
        font: Font = Font.system,
        modifier: Modifier = Modifier,
        textAlign: TextAlign? = null,
    ) {
        Material3Text(
            text = text,
            color = color,
            modifier = modifier,
            style = font.textStyle,
            textAlign = textAlign,
        )
    }

    // MARK: - Button

    /**
     * A button that displays a styled text label.
     *
     * @param text The label text.
     * @param color The color of the label.
     * @param onClick The action to perform when tapped.
     * @param font The font to apply to the label. Defaults to
     *   [Font.system].
     * @param modifier The modifier for this component.
     */
    @Composable
    fun Button(
        text: String,
        color: Color,
        onClick: () -> Unit,
        font: Font = Font.system,
        modifier: Modifier = Modifier,
    ) {
        Box(modifier = modifier.clickable(onClick = onClick)) {
            Text(
                text,
                color = color,
                font = font,
            )
        }
    }

    /**
     * A button that displays a symbol label.
     *
     * @param symbolName The SF Symbol name (mapped to a Material symbol).
     * @param color The tint of the symbol.
     * @param onClick The action to perform when tapped.
     * @param modifier The modifier for this component.
     */
    @Composable
    fun Button(
        symbolName: String,
        color: Color,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Box(modifier = modifier.clickable(onClick = onClick)) {
            Symbol(
                symbolName,
                color = color,
            )
        }
    }

    // MARK: - Capsule Button

    /**
     * A prominent, filled capsule button using the theme's accent fill.
     *
     * When disabled, it uses the theme's disabled fill and ignores
     * taps. The label uses the theme's background color for contrast.
     *
     * @param text The label text.
     * @param onClick The action to perform when tapped.
     * @param isEnabled Whether the button responds to taps.
     * @param modifier The modifier for this component.
     */
    @Composable
    fun CapsuleButton(
        text: String,
        onClick: () -> Unit,
        isEnabled: Boolean = true,
        modifier: Modifier = Modifier,
    ) {
        val colors = LocalPantherColors.current
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                modifier
                    .clip(RoundedCornerShape(CAPSULE_CORNER_RADIUS))
                    .background(if (isEnabled) colors.accent else colors.disabled)
                    .clickable(enabled = isEnabled, onClick = onClick)
                    .padding(
                        horizontal = CAPSULE_HORIZONTAL_PADDING,
                        vertical = CAPSULE_VERTICAL_PADDING,
                    ),
        ) {
            Text(text, color = colors.background, font = Font.systemSemibold())
        }
    }

    // MARK: - Symbol

    /**
     * Displays a symbol image, standing in for an SF Symbol.
     *
     * @param systemName The SF Symbol name. Unmapped names render a
     *   warning symbol.
     * @param color The tint of the symbol.
     * @param modifier The modifier for this component.
     */
    @Composable
    fun Symbol(
        systemName: String,
        color: Color,
        modifier: Modifier = Modifier,
    ) {
        Icon(
            imageVector = SFSymbol.imageVector(systemName),
            contentDescription = null,
            modifier = modifier,
            tint = color,
        )
    }
}

private val CAPSULE_CORNER_RADIUS = 24.dp
private val CAPSULE_HORIZONTAL_PADDING = 28.dp
private val CAPSULE_VERTICAL_PADDING = 14.dp
