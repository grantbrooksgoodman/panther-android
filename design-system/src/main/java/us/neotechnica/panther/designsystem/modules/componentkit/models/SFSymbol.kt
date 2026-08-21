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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
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
    fun imageVector(systemName: String): ImageVector = SYMBOLS[systemName] ?: Icons.Filled.Warning

    // MARK: - Companion

    private val SYMBOLS: Map<String, ImageVector> =
        mapOf(
            "plus" to Icons.Filled.Add,
            "checkmark" to Icons.Filled.Check,
            "xmark" to Icons.Filled.Close,
            "trash" to Icons.Filled.Delete,
            "heart" to Icons.Filled.Favorite,
            "heart.fill" to Icons.Filled.Favorite,
            "ellipsis" to Icons.Filled.MoreHoriz,
            "bell" to Icons.Filled.Notifications,
            "bell.fill" to Icons.Filled.Notifications,
            "person" to Icons.Filled.Person,
            "person.fill" to Icons.Filled.Person,
            "magnifyingglass" to Icons.Filled.Search,
            "gear" to Icons.Filled.Settings,
            "gearshape" to Icons.Filled.Settings,
            "gearshape.fill" to Icons.Filled.Settings,
            "star" to Icons.Filled.Star,
            "star.fill" to Icons.Filled.Star,
            "chevron.left" to Icons.AutoMirrored.Filled.ArrowBack,
            "chevron.right" to Icons.AutoMirrored.Filled.ArrowForward,
            "paperplane" to Icons.AutoMirrored.Filled.Send,
            "paperplane.fill" to Icons.AutoMirrored.Filled.Send,
            "doc.on.doc" to Icons.Filled.ContentCopy,
            "doc.on.doc.fill" to Icons.Filled.ContentCopy,
            "square.and.pencil" to Icons.Filled.Edit,
            "globe" to Icons.Filled.Language,
            "character.bubble" to Icons.AutoMirrored.Filled.Message,
            "character.bubble.fill" to Icons.AutoMirrored.Filled.Message,
            "arrow.clockwise" to Icons.Filled.Refresh,
            "speaker.wave.2" to Icons.Filled.VolumeUp,
            "speaker.wave.2.fill" to Icons.Filled.VolumeUp,
        )
}
