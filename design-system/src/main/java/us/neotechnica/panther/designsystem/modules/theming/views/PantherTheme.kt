//
//  PantherTheme.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService

/**
 * The active [PantherColors] palette.
 *
 * Provided by [PantherTheme]; reading it outside a [PantherTheme] is a
 * programming error.
 */
val LocalPantherColors =
    staticCompositionLocalOf<PantherColors> {
        error("LocalPantherColors accessed outside a PantherTheme")
    }

/**
 * The themed root of the app's Compose hierarchy.
 *
 * [PantherTheme] resolves the active theme and appearance from
 * [ThemeService], provides the resulting [PantherColors] through
 * [LocalPantherColors], and installs a matching Material color scheme.
 * It recomposes automatically when the theme, the appearance override,
 * or the system appearance changes – the Compose equivalent of the iOS
 * `ThemedView` redraw-on-appearance-change behavior.
 *
 * @param content The themed content.
 */
@Composable
fun PantherTheme(content: @Composable () -> Unit) {
    val theme by ThemeService.currentTheme.collectAsState()
    val styleOverride by ThemeService.styleOverride.collectAsState()
    val systemInDarkMode = isSystemInDarkTheme()

    // Recompute on any of the three inputs.
    val isDark =
        remember(theme, styleOverride, systemInDarkMode) {
            ThemeService.isDarkModeActive(systemInDarkMode)
        }

    val colors = remember(theme, isDark) { PantherColors(theme, isDark) }

    val colorScheme =
        remember(colors, isDark) {
            val base = if (isDark) darkColorScheme() else lightColorScheme()
            base.copy(
                primary = colors.accent,
                onPrimary = colors.navigationBarTitle,
                background = colors.background,
                onBackground = colors.titleText,
                surface = colors.groupedContentBackground,
                onSurface = colors.titleText,
                surfaceVariant = colors.groupedContentBackground,
                onSurfaceVariant = colors.subtitleText,
            )
        }

    CompositionLocalProvider(LocalPantherColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * A container whose content reflects the active theme.
 *
 * In Compose, theme colors flow from the [PantherTheme] root through
 * [LocalPantherColors], so [ThemedView] simply renders its content; it
 * exists for call-site parity with the iOS `ThemedView`.
 *
 * @param content The themed content.
 */
@Composable
fun ThemedView(content: @Composable () -> Unit) {
    content()
}
