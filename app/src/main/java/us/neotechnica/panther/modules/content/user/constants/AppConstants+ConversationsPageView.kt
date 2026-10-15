//
//  AppConstants+ConversationsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("VariableMaxLength")

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService

// MARK: - Float

/** The layout and timing values of the conversations page view. */
object ConversationsPageViewFloats {
    /** The scale the compose toolbar button animates to. */
    const val COMPOSE_TOOLBAR_BUTTON_ANIMATION_AMOUNT = 1.4f

    /** The delay, in seconds, before the compose toolbar button's animation. */
    const val COMPOSE_TOOLBAR_BUTTON_ANIMATION_DELAY = 0.1f

    /** The duration, in seconds, of the compose toolbar button's animation. */
    const val COMPOSE_TOOLBAR_BUTTON_ANIMATION_DURATION = 0.4f

    /** The interval, in seconds, between contact syncs. */
    const val CONTACT_SYNC_INTERVAL_SECONDS = 3600

    /** The minimum frame height of the toolbar button. */
    val toolbarButtonFrameMinHeight: Dp = 30.dp

    /** The minimum frame width of the toolbar button. */
    val toolbarButtonFrameMinWidth: Dp = 30.dp

    /** The horizontal inset of the toolbar row. */
    val headerHorizontalPadding: Dp = 16.dp

    /** The top inset of the toolbar row. */
    val headerTopPadding: Dp = 12.dp

    /**
     * The distance the list must be pulled past its top before a
     * refresh is requested. Set above the platform default so that
     * scrolling back to the top does not trigger a refresh
     * unintentionally.
     */
    val pullToRefreshThreshold: Dp = 120.dp

    /** The spacing below the search bar. It closes while a search is under way. */
    val searchBottomSpacing: Dp = 8.dp

    /**
     * The spacing above the search bar while a search is under way,
     * once the toolbar and title have collapsed.
     */
    val searchTopSpacing: Dp = 8.dp

    /** The horizontal inset of the title. */
    val titleHorizontalPadding: Dp = 20.dp

    /** The vertical inset of the title. */
    val titleVerticalPadding: Dp = 8.dp
}

// MARK: - Color

/** The colors of the conversations page view. */
object ConversationsPageViewColors {
    /** The foreground color of the create random messages toolbar button. */
    val createRandomMessagesToolbarButtonForeground = Color(0xFFAF52DE)

    /** The foreground color of the delete conversations toolbar button. */
    val deleteConversationsToolbarButtonForeground = Color(0xFFFF3B30)

    /** The foreground color of the storage full toolbar button. */
    val storageFullToolbarButtonForeground = Color(0xFFFF3B30)

    /** The compose button's foreground: white outside the default theme, system blue within it. */
    val composeToolbarButtonForeground: Color
        get() = if (!ThemeService.isAppDefaultThemeApplied) Color.White else SYSTEM_BLUE

    /** The settings button's foreground: white outside the default theme, system blue within it. */
    val settingsToolbarButtonForeground: Color
        get() = if (!ThemeService.isAppDefaultThemeApplied) Color.White else SYSTEM_BLUE

    private val SYSTEM_BLUE = Color(0xFF007AFF)
}

// MARK: - String

/** The string constants of the conversations page view. */
object ConversationsPageViewConstants {
    /** The system name of the create random messages toolbar button image. */
    const val CREATE_RANDOM_MESSAGES_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "sparkles.2"

    /** The system name of the delete conversations toolbar button image. */
    const val DELETE_CONVERSATIONS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "trash"

    /** The system name of the storage full toolbar button image. */
    const val STORAGE_FULL_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.triangle"

    /** The system name of the compose toolbar button image. */
    const val COMPOSE_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "square.and.pencil"

    /** The system name of the settings toolbar button image. */
    const val SETTINGS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "gearshape"
}
