//
//  SFSymbol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight

/**
 * Resolves SF Symbol names to images.
 *
 * A name resolves to the real SF Symbol while [usesSFSymbols] is
 * `true` and the symbol is bundled. Otherwise, it resolves to its
 * nearest Material Symbol. Unknown names resolve to a warning symbol.
 */
object SFSymbol {
    // MARK: - Types

    /** The size of a symbol relative to the text it accompanies. */
    enum class Scale {
        /** A symbol smaller than the text it accompanies. */
        SMALL,

        /** A symbol sized to the text it accompanies. */
        MEDIUM,
    }

    // MARK: - Properties

    /**
     * A Boolean value that indicates whether names resolve to real SF
     * Symbols, where bundled, rather than to Material Symbols.
     *
     * The default is `true`.
     *
     * **Important:** Set this value once at launch, before any content
     * is composed. Symbols already on screen don't update when it
     * changes.
     */
    var usesSFSymbols = true

    // MARK: - Methods

    /**
     * Returns the [ImageVector] for the given SF Symbol name.
     *
     * The image is the real SF Symbol while [usesSFSymbols] is `true`
     * and the symbol is bundled – in the given weight and scale where
     * that variant is bundled, and in the regular weight and medium
     * scale otherwise. Failing that, it is the nearest Material Symbol,
     * which has no weight or scale, or a warning symbol if the name is
     * not mapped.
     *
     * @param systemName The SF Symbol name.
     * @param weight The weight of the symbol, or `null` for the regular
     *   weight.
     * @param scale The scale of the symbol. The default is
     *   [Scale.MEDIUM].
     *
     * @return The corresponding image vector.
     */
    @Composable
    fun imageVector(
        systemName: String,
        weight: FontWeight? = null,
        scale: Scale = Scale.MEDIUM,
    ): ImageVector {
        val drawableID =
            SFSymbolVariantDrawables[SFSymbolVariant(systemName, weight, scale)] ?: SFSymbolDrawables[systemName]
        if (usesSFSymbols && drawableID != null) return ImageVector.vectorResource(drawableID)
        return SYMBOLS[systemName] ?: Icons.Filled.Warning
    }

    /**
     * Returns a Boolean value that indicates whether the given name
     * resolves to a real SF Symbol rather than to a Material Symbol.
     *
     * An SF Symbol fills its bounds, while a Material Symbol is drawn
     * with padding inside them. Use this method to size a symbol whose
     * layout compensates for that padding.
     *
     * @param systemName The SF Symbol name.
     *
     * @return `true` if the name resolves to a real SF Symbol;
     *   otherwise, `false`.
     */
    fun resolvesToSFSymbol(systemName: String): Boolean = usesSFSymbols && systemName in SFSymbolDrawables

    // MARK: - Companion

    private val SYMBOLS: Map<String, ImageVector> =
        mapOf(
            "plus" to Icons.Filled.Add,
            "plus.circle" to Icons.Filled.AddCircle,
            "arrow.up" to Icons.Filled.ArrowUpward,
            "checkmark" to Icons.Filled.Check,
            "xmark" to Icons.Filled.Close,
            "trash" to Icons.Filled.Delete,
            "trash.fill" to Icons.Filled.Delete,
            "hand.raised.fill" to Icons.Filled.PanTool,
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
            "stop.fill" to Icons.Filled.Stop,
            "arrow.up.circle.fill" to Icons.Filled.ArrowCircleUp,
            "square.and.arrow.down" to Icons.Filled.Download,
            "square.and.pencil" to Icons.Filled.Edit,
            "globe" to Icons.Filled.Language,
            "exclamationmark.bubble" to Icons.Filled.Feedback,
            "flag" to Icons.Filled.Flag,
            "flag.fill" to Icons.Filled.Flag,
            "location.fill" to Icons.Filled.NearMe,
            "info" to Icons.Filled.Info,
            "info.circle" to Icons.Filled.Info,
            "exclamationmark.circle" to Icons.Filled.Error,
            "person.crop.circle.fill" to Icons.Filled.AccountCircle,
            "square.text.square.fill" to Icons.Filled.TextFields,
            "arrow.counterclockwise" to Icons.Filled.Replay,
            "checkmark.circle.fill" to Icons.Filled.CheckCircle,
            "xmark.circle.fill" to Icons.Filled.Cancel,
            "command" to Icons.Filled.KeyboardCommandKey,
            "character.bubble" to Icons.AutoMirrored.Filled.Message,
            "character.bubble.fill" to Icons.AutoMirrored.Filled.Message,
            "text.bubble" to Icons.AutoMirrored.Filled.Message,
            "text.bubble.fill" to Icons.AutoMirrored.Filled.Message,
            "arrow.clockwise" to Icons.Filled.Refresh,
            "speaker.wave.2" to Icons.AutoMirrored.Filled.VolumeUp,
            "speaker.wave.2.fill" to Icons.AutoMirrored.Filled.VolumeUp,
            "speaker.wave.2.circle" to Icons.AutoMirrored.Filled.VolumeUp,
            "speaker.wave.2.bubble" to Icons.AutoMirrored.Filled.VolumeUp,
            "speaker.slash.circle" to Icons.AutoMirrored.Filled.VolumeOff,
            "chevron.forward" to Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "chevron.backward" to Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            "chevron.compact.right" to Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "chevron.right.circle" to Icons.AutoMirrored.Filled.KeyboardArrowRight,
            "chevron.down.circle" to Icons.Filled.KeyboardArrowDown,
            "person.2.circle.fill" to Icons.Filled.Group,
            "doc.circle.fill" to Icons.Filled.Description,
            "wifi.slash" to Icons.Filled.WifiOff,
            "externaldrive.trianglebadge.exclamationmark" to Icons.Filled.Warning,
            "exclamationmark.triangle" to Icons.Filled.Warning,
            "exclamationmark.triangle.fill" to Icons.Filled.Warning,
            "exclamationmark.octagon.fill" to Icons.Filled.Error,
            "info.circle.fill" to Icons.Filled.Info,
            "x.circle.fill" to Icons.Filled.Cancel,
            "questionmark" to Icons.Filled.QuestionMark,
            "questionmark.circle.fill" to Icons.AutoMirrored.Filled.Help,
            "questionmark.square.dashed" to Icons.Filled.Warning,
        )
}
