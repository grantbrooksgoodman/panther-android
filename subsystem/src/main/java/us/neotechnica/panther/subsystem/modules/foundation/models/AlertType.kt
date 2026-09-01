//
//  AlertType.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.foundation.models

import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * The kind of user-visible alert to present alongside a log entry.
 *
 * When you pass an [AlertType] to one of [Logger]'s `log` methods,
 * the logger forwards it to the registered presentation delegate,
 * which displays the corresponding alert after the entry is
 * recorded. Use [ErrorAlert] for reportable exceptions,
 * [NormalAlert] for informational messages, or [Toast] for
 * lightweight, non-blocking feedback.
 */
sealed interface AlertType {
    /**
     * An error alert that offers to file a report when the
     * exception is reportable.
     */
    data object ErrorAlert : AlertType

    /** A standard informational alert. */
    data object NormalAlert : AlertType

    /**
     * A toast notification.
     *
     * @property style The visual style of the toast. Pass `null`
     *   to infer the style from the log entry.
     * @property isPersistent Whether the toast remains on screen
     *   until manually dismissed. The default is `true`.
     */
    data class Toast(
        val style: ToastStyle? = null,
        val isPersistent: Boolean = true,
    ) : AlertType

    // MARK: - Companion

    companion object {
        /**
         * A toast notification with the default style and
         * persistent display.
         */
        val toast: AlertType = Toast()

        /**
         * A toast notification that is only shown in prerelease
         * builds.
         *
         * Returns `null` when the current build milestone is
         * [Milestone.GENERAL_RELEASE], effectively silencing the
         * alert in production.
         */
        val toastInPrerelease: AlertType?
            get() = toastInPrerelease()

        /**
         * Returns a toast notification with the given style, or
         * `null` in general-release builds.
         *
         * @param style The visual style of the toast. Pass `null`
         *   to infer the style.
         * @param isPersistent Whether the toast remains on screen
         *   until manually dismissed. The default is `true`.
         *
         * @return An [AlertType] in prerelease builds, or `null`
         *   in general-release builds.
         */
        fun toastInPrerelease(
            style: ToastStyle? = null,
            isPersistent: Boolean = true,
        ): AlertType? {
            if (Build.milestone == Milestone.GENERAL_RELEASE) return null
            return Toast(style = style, isPersistent = isPersistent)
        }
    }
}
