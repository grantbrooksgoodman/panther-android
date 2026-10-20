//
//  PantherTheme.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.views

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.view.WindowCompat
import us.neotechnica.panther.designsystem.modules.foundation.components.CompatibilityFrame
import us.neotechnica.panther.designsystem.modules.foundation.components.compatibilityFrameWidth
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
 * It also styles the system bar icons to remain legible against the
 * active appearance, and the status bar icons against the black area
 * outside a [CompatibilityFrame] on a large window. It recomposes
 * automatically when the theme, the appearance override, or the
 * system appearance changes.
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

    SystemBarAppearanceEffect(isDark)
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
 * [LocalPantherColors], so [ThemedView] renders its content.
 *
 * @param content The themed content.
 */
@Composable
fun ThemedView(content: @Composable () -> Unit) {
    content()
}

// MARK: - Auxiliary

// Styles the hosting activity's system bar icons for the given
// appearance – light icons in dark mode, dark icons otherwise. On a
// large window, the status bar's icons sit over the black area outside
// the compatibility frame, so they are light in either appearance. The
// activity handles appearance changes without being recreated, so the
// icons are restyled on each change.
@Composable
private fun SystemBarAppearanceEffect(isDarkModeActive: Boolean) {
    val isFramed = compatibilityFrameWidth(LocalWindowInfo.current.containerDpSize) != null
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightNavigationBars = !isDarkModeActive
            isAppearanceLightStatusBars = !isDarkModeActive && !isFramed
        }
    }
}
