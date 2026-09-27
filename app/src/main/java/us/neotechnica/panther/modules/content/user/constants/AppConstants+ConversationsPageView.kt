//
//  AppConstants+ConversationsPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object ConversationsPageViewFloats {
    val headerHorizontalPadding: Dp = 16.dp
    val headerTopPadding: Dp = 12.dp

    /**
     * The distance the list must be pulled past its top before a
     * refresh is requested. Set above the platform default so that
     * scrolling back to the top does not trigger a refresh
     * unintentionally.
     */
    val pullToRefreshThreshold: Dp = 120.dp
    val searchBottomSpacing: Dp = 8.dp
    val searchHorizontalPadding: Dp = 16.dp
    val titleHorizontalPadding: Dp = 20.dp
    val titleVerticalPadding: Dp = 8.dp
}
