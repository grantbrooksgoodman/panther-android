//
//  SFSymbol.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the SF Symbol names used by the iOS app to their nearest
 * Material Symbol, standing in for SF Symbols (which are unavailable on
 * Android).
 *
 * Unknown names resolve to a warning symbol, mirroring the iOS
 * fallback. Extend the mapping as later phases introduce new symbols.
 */
object SFSymbol {
    // MARK: - Methods

    /**
     * Returns the Material [ImageVector] for the given SF Symbol name,
     * or a warning symbol if the name is not mapped.
     *
     * @param systemName The SF Symbol name.
     *
     * @return The corresponding image vector.
     */
    fun imageVector(systemName: String): ImageVector =
        when (systemName) {
            "plus" -> Icons.Filled.Add
            "checkmark" -> Icons.Filled.Check
            "xmark" -> Icons.Filled.Close
            "trash" -> Icons.Filled.Delete
            "heart", "heart.fill" -> Icons.Filled.Favorite
            "ellipsis" -> Icons.Filled.MoreHoriz
            "bell", "bell.fill" -> Icons.Filled.Notifications
            "person", "person.fill" -> Icons.Filled.Person
            "magnifyingglass" -> Icons.Filled.Search
            "gear", "gearshape", "gearshape.fill" -> Icons.Filled.Settings
            "star", "star.fill" -> Icons.Filled.Star
            "chevron.left" -> Icons.AutoMirrored.Filled.ArrowBack
            "chevron.right" -> Icons.AutoMirrored.Filled.ArrowForward
            "paperplane", "paperplane.fill" -> Icons.AutoMirrored.Filled.Send
            else -> Icons.Filled.Warning
        }
}
