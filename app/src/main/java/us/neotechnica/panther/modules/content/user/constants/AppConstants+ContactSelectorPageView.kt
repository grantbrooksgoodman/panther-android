//
//  AppConstants+ContactSelectorPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 24/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

object ContactSelectorPageViewFloats {
    val doneButtonGlyphSize: Dp = 18.dp
    val emptyStateHorizontalPadding: Dp = 40.dp
    val emptyStateVerticalPadding: Dp = 24.dp
    val headerHorizontalPadding: Dp = 16.dp
    val headerVerticalPadding: Dp = 12.dp
    val searchHorizontalPadding: Dp = 16.dp
    val searchVerticalPadding: Dp = 4.dp
}

// MARK: - String

object ContactSelectorPageViewStrings {
    const val CLOSE = "Close"
    const val NO_CONTACTS_FOUND = "No contacts found.\nTap to search for users with this phone number."
    const val NO_RESULTS = "No Results"
    const val SEARCH_PLACEHOLDER = "Search contacts or enter phone number"
    const val TITLE = "Contacts"
}
