//
//  AlertKitPalette.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertKitPaletteColors
import us.neotechnica.panther.designsystem.modules.alertkit.constants.AlertKitPaletteFloats
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService

// MARK: - Constants Accessors

private typealias PaletteColors = AlertKitPaletteColors
private typealias PaletteFloats = AlertKitPaletteFloats

// The colors of alerts and action sheets for the given appearance.
internal class AlertKitPalette(
    val isDarkModeActive: Boolean,
) {
    // MARK: - Properties

    val actionLabel = color(PaletteColors.ACTION_LABEL_LIGHT, PaletteColors.ACTION_LABEL_DARK)
    val actionLabelDestructive = color(PaletteColors.ACTION_LABEL_DESTRUCTIVE_LIGHT, PaletteColors.ACTION_LABEL_DESTRUCTIVE_DARK)
    val actionLabelDisabled = color(PaletteColors.ACTION_LABEL_DISABLED_LIGHT, PaletteColors.ACTION_LABEL_DISABLED_DARK)
    val background = color(PaletteColors.BACKGROUND_LIGHT, PaletteColors.BACKGROUND_DARK)
    val cancelButtonBackground = color(PaletteColors.CANCEL_BUTTON_BACKGROUND_LIGHT, PaletteColors.CANCEL_BUTTON_BACKGROUND_DARK)
    val clearButton = color(PaletteColors.CLEAR_BUTTON_LIGHT, PaletteColors.CLEAR_BUTTON_DARK)
    val cursor = PaletteColors.CURSOR
    val dimming = PaletteColors.DIMMING.copy(alpha = PaletteFloats.DIMMING_OPACITY)
    val highlight = color(PaletteColors.HIGHLIGHT_LIGHT, PaletteColors.HIGHLIGHT_DARK).copy(alpha = PaletteFloats.HIGHLIGHT_OPACITY)
    val label = color(PaletteColors.LABEL_LIGHT, PaletteColors.LABEL_DARK)
    val placeholder = color(PaletteColors.PLACEHOLDER_LIGHT, PaletteColors.PLACEHOLDER_DARK)
    val progressFill = color(PaletteColors.PROGRESS_FILL_LIGHT, PaletteColors.PROGRESS_FILL_DARK)
    val progressTrack = color(PaletteColors.PROGRESS_TRACK_LIGHT, PaletteColors.PROGRESS_TRACK_DARK)
    val secondaryLabel = color(PaletteColors.SECONDARY_LABEL_LIGHT, PaletteColors.SECONDARY_LABEL_DARK)
    val separator = color(PaletteColors.SEPARATOR_LIGHT, PaletteColors.SEPARATOR_DARK)
    val textFieldBackground = color(PaletteColors.TEXT_FIELD_BACKGROUND_LIGHT, PaletteColors.TEXT_FIELD_BACKGROUND_DARK)

    // MARK: - Auxiliary

    private fun color(
        light: Color,
        dark: Color,
    ): Color = if (isDarkModeActive) dark else light

    // MARK: - Companion

    companion object {
        // The palette for the current appearance.
        val current: AlertKitPalette
            @Composable get() = AlertKitPalette(ThemeService.isDarkModeActive(isSystemInDarkTheme()))
    }
}
