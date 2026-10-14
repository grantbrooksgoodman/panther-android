//
//  ContextMenuAction.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * A single reaction option in a message context menu's reaction row.
 *
 * @property emoji The emoji that represents the reaction style.
 * @property selectedColor The background color shown when the current
 *   user has applied this reaction.
 * @property isSelected Whether the current user has applied this reaction.
 * @property isDoubleTapDefault Whether double-tapping the message applies
 *   this reaction.
 * @property onSelect The closure to run when the user chooses this
 *   reaction.
 */
data class ReactionChoice(
    val emoji: String,
    val selectedColor: Color,
    val isSelected: Boolean,
    val isDoubleTapDefault: Boolean = false,
    val onSelect: () -> Unit,
)

/**
 * A single action in a message context menu.
 *
 * @property title The action's title.
 * @property systemImageName The SF Symbol name for the action's icon.
 * @property isDestructive Whether the action is destructive (rendered in
 *   red).
 * @property imageResourceID An image resource for the action's icon,
 *   drawn as a template in place of the symbol named by
 *   [systemImageName], or `null` to draw the symbol.
 * @property onSelect The closure to run when the action is chosen.
 */
data class ContextMenuAction(
    val title: String,
    val systemImageName: String,
    val isDestructive: Boolean = false,
    @DrawableRes val imageResourceID: Int? = null,
    val onSelect: () -> Unit,
)

/** The side a context menu anchors to, following its message bubble. */
enum class ContextMenuAlignment {
    LEADING,
    TRAILING,
}
