//
//  AppConstants+UserContentContainerView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/** The layout and timing values of the user content container view. */
object UserContentContainerViewFloats {
    /** The minimum frame height of the chat info toolbar button. */
    val chatInfoToolbarButtonFrameMinHeight: Dp = 30.dp

    /** The minimum frame width of the chat info toolbar button. */
    val chatInfoToolbarButtonFrameMinWidth: Dp = 30.dp

    /** The height of the chat page header content. */
    val chatPageHeaderContentHeight: Dp = 105.dp

    /** The amount added to the chat page header's content height. */
    val chatPageHeaderContentHeightIncrement: Dp = 7.dp
}

// MARK: - String

/** The string constants of the user content container view. */
object UserContentContainerViewStrings {
    /** The system name of the chat info button image. */
    const val CHAT_INFO_BUTTON_IMAGE_SYSTEM_NAME = "info.circle"
}
