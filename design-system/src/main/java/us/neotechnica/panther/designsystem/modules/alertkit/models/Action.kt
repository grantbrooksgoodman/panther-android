//
//  Action.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

/**
 * A value that represents a button in an alert.
 *
 * An [Action] pairs a title with an effect that executes when the user
 * taps the corresponding button, and a [style] that determines its
 * visual treatment.
 *
 * ```kotlin
 * val action = Action("Delete", style = ActionStyle.DESTRUCTIVE) {
 *     deleteItem()
 * }
 * ```
 */
class Action(
    /** The title displayed on the action's button. */
    val title: String,
    /** Whether the action is enabled. */
    val isEnabled: Boolean = true,
    /** The style applied to the action's button. */
    val style: ActionStyle = ActionStyle.DEFAULT,
    /** The effect to run when the user taps the button. */
    val effect: () -> Unit,
)
