//
//  ThemeService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.designsystem.modules.theming.extensions.themedViewAppearanceChanged
import us.neotechnica.panther.designsystem.modules.theming.models.Theme
import us.neotechnica.panther.designsystem.modules.theming.models.ThemeStyle
import us.neotechnica.panther.designsystem.modules.theming.models.Themes
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.send

/**
 * The central point of control for the app's active theme.
 *
 * Use [ThemeService] to read the current theme, apply a new one,
 * force a light or dark appearance, or suppress Material ripples.
 * Theme changes are published through
 * [currentTheme] and [styleOverride], which
 * [PantherTheme][us.neotechnica.panther.designsystem.modules.theming.views.PantherTheme]
 * collects to recompose the themed view tree, and are announced through
 * the `themedViewAppearanceChanged` shared event.
 *
 * **Note:** The selected theme is held in memory and does not persist
 * across launches.
 */
object ThemeService {
    // MARK: - Properties

    /**
     * A Boolean value that indicates whether Material ripples are
     * suppressed throughout the themed view tree.
     *
     * While this is `true`, no component shows a ripple when it is
     * pressed – neither a clickable modifier nor a Material component
     * such as a switch or a button. Components that draw their own
     * press feedback are unaffected. The default is `true`.
     *
     * **Important:** Set this value once at launch, before any content
     * is composed. Content already on screen doesn't update when it
     * changes.
     */
    var suppressesRipples = true

    private val mutableCurrentTheme = MutableStateFlow(Themes.appDefault)
    private val mutableStyleOverride = MutableStateFlow<ThemeStyle?>(null)

    // MARK: - Computed Properties

    /** The currently active theme. */
    val currentTheme: StateFlow<Theme> = mutableCurrentTheme.asStateFlow()

    /**
     * A Boolean value that indicates whether the app's default theme
     * is the active theme.
     */
    val isAppDefaultThemeApplied: Boolean
        get() = mutableCurrentTheme.value == Themes.appDefault

    /**
     * An appearance override applied over the active theme's style, or
     * `null` to follow the theme.
     */
    val styleOverride: StateFlow<ThemeStyle?> = mutableStyleOverride.asStateFlow()

    // MARK: - Methods

    /**
     * Returns a Boolean value that indicates whether dark mode is
     * currently active, accounting for the style override, the active
     * theme's style, and the system appearance.
     *
     * @param systemInDarkMode Whether the system is in dark mode.
     *
     * @return `true` if dark mode is active.
     */
    fun isDarkModeActive(systemInDarkMode: Boolean): Boolean =
        when (mutableStyleOverride.value ?: mutableCurrentTheme.value.style) {
            ThemeStyle.DARK -> true
            ThemeStyle.LIGHT -> false
            ThemeStyle.UNSPECIFIED -> systemInDarkMode
        }

    /**
     * Applies a new theme and announces the change.
     *
     * @param theme The theme to activate.
     */
    fun setTheme(theme: Theme) {
        mutableCurrentTheme.value = theme
        announceChange()
    }

    /**
     * Forces a light or dark appearance, or clears the override.
     *
     * @param style The appearance to force, or `null` to follow the
     *   active theme.
     */
    fun setStyleOverride(style: ThemeStyle?) {
        mutableStyleOverride.value = style
        announceChange()
    }

    // MARK: - Auxiliary

    private fun announceChange() {
        DependencyValues.current.sharedEvents.themedViewAppearanceChanged
            .send()
    }
}
