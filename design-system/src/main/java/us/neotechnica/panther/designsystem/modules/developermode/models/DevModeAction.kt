//
//  DevModeAction.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 07/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.developermode.models

/**
 * A single action that appears in the Developer Mode menu.
 *
 * Create a [DevModeAction] to expose a debug or diagnostic operation to
 * developers at runtime. Each action has a title, an optional
 * destructive flag, and a closure that performs the work.
 *
 * Register the action with
 * [DevModeService][us.neotechnica.panther.designsystem.modules.developermode.services.DevModeService]
 * to make it available in the Developer Mode action sheet.
 *
 * Two actions are considered equal when their [title] and
 * [isDestructive] properties match. Adding an action whose metadata
 * matches an existing one replaces the original.
 *
 * @property title The display title shown in the Developer Mode action
 *   sheet.
 * @property isDestructive A Boolean value that indicates whether the
 *   action is presented with a destructive style.
 * @property perform The closure to execute when the developer selects
 *   this action.
 */
class DevModeAction(
    val title: String,
    val isDestructive: Boolean = false,
    val perform: () -> Unit,
) {
    // MARK: - Companion

    companion object

    // MARK: - Equality Comparison

    /**
     * Returns a Boolean value indicating whether this action's metadata
     * matches the given action's metadata.
     *
     * Two actions are considered equal when both their [title] and
     * [isDestructive] properties are identical. The [perform] closure is
     * not compared.
     *
     * @param action The action to compare against.
     *
     * @return `true` if the metadata matches; otherwise, `false`.
     */
    fun metadata(isEqual: DevModeAction): Boolean = title == isEqual.title && isDestructive == isEqual.isDestructive

    /**
     * Returns a Boolean value indicating whether this action's metadata
     * matches the given title and destructive flag.
     *
     * @param title The title to compare against.
     * @param isDestructive The destructive flag to compare against.
     *
     * @return `true` if the metadata matches; otherwise, `false`.
     */
    fun metadata(
        title: String,
        isDestructive: Boolean,
    ): Boolean = this.title == title && this.isDestructive == isDestructive
}
