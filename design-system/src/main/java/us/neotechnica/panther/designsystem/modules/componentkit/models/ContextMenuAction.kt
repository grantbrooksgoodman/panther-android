//
//  ContextMenuAction.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

/**
 * A single action in a message context menu.
 *
 * @property title The action's title.
 * @property systemImageName The SF Symbol name for the action's icon.
 * @property isDestructive Whether the action is destructive (rendered in
 *   red).
 * @property onSelect The closure to run when the action is chosen.
 */
data class ContextMenuAction(
    val title: String,
    val systemImageName: String,
    val isDestructive: Boolean = false,
    val onSelect: () -> Unit,
)

/** The side a context menu anchors to, following its message bubble. */
enum class ContextMenuAlignment {
    LEADING,
    TRAILING,
}
