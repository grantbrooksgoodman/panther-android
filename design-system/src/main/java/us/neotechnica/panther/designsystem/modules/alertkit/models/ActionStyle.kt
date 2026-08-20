//
//  ActionStyle.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.alertkit.models

/**
 * Constants that indicate the visual style of an action's button.
 *
 * Use [DESTRUCTIVE] for actions that delete data and [CANCEL] for
 * actions that dismiss the alert without changes. [PREFERRED] and
 * [DESTRUCTIVE_PREFERRED] give the button visual emphasis.
 */
enum class ActionStyle {
    CANCEL,
    DEFAULT,
    DESTRUCTIVE,
    DESTRUCTIVE_PREFERRED,
    PREFERRED,
    ;

    // MARK: - Computed Properties

    /** Whether this style marks the alert's preferred action. */
    val isPreferred: Boolean
        get() = this == PREFERRED || this == DESTRUCTIVE_PREFERRED

    /** Whether this style denotes a destructive action. */
    val isDestructive: Boolean
        get() = this == DESTRUCTIVE || this == DESTRUCTIVE_PREFERRED
}
