//
//  AppConstants+ConversationsPageView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService

// MARK: - Float

object ConversationsPageViewFloats {
    const val COMPOSE_TOOLBAR_BUTTON_ANIMATION_AMOUNT = 1.4f
    const val COMPOSE_TOOLBAR_BUTTON_ANIMATION_DELAY = 0.1f
    const val COMPOSE_TOOLBAR_BUTTON_ANIMATION_DURATION = 0.4f
    const val CONTACT_SYNC_INTERVAL_SECONDS = 3600
    val toolbarButtonFrameMinHeight: Dp = 30.dp
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

object ConversationsPageViewColors {
    val createRandomMessagesToolbarButtonForeground = Color(0xFFAF52DE)
    val deleteConversationsToolbarButtonForeground = Color(0xFFFF3B30)

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

object ConversationsPageViewConstants {
    const val CREATE_RANDOM_MESSAGES_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "sparkles.2"
    const val DELETE_CONVERSATIONS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "trash"

    const val STORAGE_FULL_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "exclamationmark.triangle"

    const val COMPOSE_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "square.and.pencil"
    const val SETTINGS_TOOLBAR_BUTTON_IMAGE_SYSTEM_NAME = "gearshape"
}
