//
//  ToastStyle.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

/**
 * The semantic style of a toast, which determines its icon and
 * default color.
 *
 * Each case represents the severity a toast conveys. Use [NONE]
 * when the toast does not require an icon.
 *
 * This type lives in the subsystem so that both [Logger] and the
 * design system's toast host can refer to the same styles across
 * the module boundary.
 */
enum class ToastStyle {
    /** An error condition that requires the user's attention. */
    ERROR,

    /** A neutral informational message. */
    INFO,

    /** A confirmation that an operation completed successfully. */
    SUCCESS,

    /** A cautionary notice. */
    WARNING,

    /** No semantic style. The toast displays without an icon. */
    NONE,
}
