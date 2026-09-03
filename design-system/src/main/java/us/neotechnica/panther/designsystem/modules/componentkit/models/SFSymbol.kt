//
//  SFSymbol.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
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
            "arrow.up" to Icons.Filled.ArrowUpward,
            "checkmark" to Icons.Filled.Check,
            "xmark" to Icons.Filled.Close,
            "trash" to Icons.Filled.Delete,
            "trash.fill" to Icons.Filled.Delete,
            "hand.raised.fill" to Icons.AutoMirrored.Filled.Logout,
            "heart" to Icons.Filled.Favorite,
            "heart.fill" to Icons.Filled.Favorite,
            "ellipsis" to Icons.Filled.MoreHoriz,
            "bell" to Icons.Filled.Notifications,
            "bell.fill" to Icons.Filled.Notifications,
            "person" to Icons.Filled.Person,
            "person.fill" to Icons.Filled.Person,
            "person.2" to Icons.Filled.Group,
            "person.2.fill" to Icons.Filled.Group,
            "magnifyingglass" to Icons.Filled.Search,
            "gear" to Icons.Filled.Settings,
            "gearshape" to Icons.Filled.Settings,
            "gearshape.fill" to Icons.Filled.Settings,
            "star" to Icons.Filled.Star,
            "star.fill" to Icons.Filled.Star,
            "chevron.left" to Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            "chevron.right" to Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "chevron.down" to Icons.Filled.KeyboardArrowDown,
            "paperplane" to Icons.AutoMirrored.Filled.Send,
            "paperplane.fill" to Icons.AutoMirrored.Filled.Send,
            "doc.on.doc" to Icons.Filled.ContentCopy,
            "doc.on.doc.fill" to Icons.Filled.ContentCopy,
            "doc" to Icons.Filled.Description,
            "doc.fill" to Icons.Filled.Description,
            "doc.text" to Icons.Filled.Description,
            "doc.text.fill" to Icons.Filled.Description,
            "paperclip" to Icons.Filled.AttachFile,
            "photo" to Icons.Filled.Image,
            "photo.fill" to Icons.Filled.Image,
            "film" to Icons.Filled.Movie,
            "film.fill" to Icons.Filled.Movie,
            "play.fill" to Icons.Filled.PlayArrow,
            "play.circle.fill" to Icons.Filled.PlayArrow,
            "pause.fill" to Icons.Filled.Pause,
            "square.and.arrow.down" to Icons.Filled.Download,
            "square.and.pencil" to Icons.Filled.Edit,
            "globe" to Icons.Filled.Language,
            "flag" to Icons.Filled.Flag,
            "character.bubble" to Icons.AutoMirrored.Filled.Message,
            "character.bubble.fill" to Icons.AutoMirrored.Filled.Message,
            "arrow.clockwise" to Icons.Filled.Refresh,
            "speaker.wave.2" to Icons.Filled.VolumeUp,
            "speaker.wave.2.fill" to Icons.Filled.VolumeUp,
            "speaker.wave.2.circle" to Icons.Filled.VolumeUp,
            "speaker.slash.circle" to Icons.Filled.VolumeOff,
        )
}
