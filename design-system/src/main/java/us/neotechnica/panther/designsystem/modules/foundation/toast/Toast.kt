//
//  Toast.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.toast

import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import kotlin.time.Duration

/**
 * A lightweight, non-modal notification that appears over the
 * current content.
 *
 * Use [Toast] to present brief messages – such as confirmation of
 * an action, a warning, or an error – without interrupting the
 * user's workflow. A toast can appear as a full-width banner or a
 * compact capsule:
 *
 * ```kotlin
 * Toast.show(Toast(Toast.Type.Banner(ToastStyle.SUCCESS), message = "Item saved."))
 * Toast.show(Toast(Toast.Type.Capsule(ToastStyle.ERROR), message = "Upload failed."))
 * ```
 *
 * By default a toast is [Perpetuation.Persistent] and remains on
 * screen until the user dismisses it. Use [Perpetuation.Ephemeral]
 * to auto-dismiss after a specified duration.
 *
 * @property type The presentation type. The default is a plain
 *   banner.
 * @property title An optional headline. Pass `null` to omit the
 *   title.
 * @property message The body text to display.
 * @property perpetuation The duration strategy. The default is
 *   [Perpetuation.Persistent].
 */
data class Toast(
    val type: Type = Type.Banner(),
    val title: String? = null,
    val message: String,
    val perpetuation: Perpetuation = Perpetuation.Persistent,
) {
    // MARK: - Types

    /**
     * The visual presentation type of a toast.
     *
     * A toast can appear as either a full-width [Banner] or a
     * compact [Capsule].
     */
    sealed interface Type {
        /** The semantic style, which determines the icon and color. */
        val style: ToastStyle

        /**
         * A full-width banner that slides in from the top edge.
         *
         * @property style The semantic style. The default is
         *   [ToastStyle.NONE].
         * @property showsDismissButton Whether to show a dismiss
         *   button. The default is `true`.
         */
        data class Banner(
            override val style: ToastStyle = ToastStyle.NONE,
            val showsDismissButton: Boolean = true,
        ) : Type

        /**
         * A compact, pill-shaped notification.
         *
         * @property style The semantic style. The default is
         *   [ToastStyle.NONE].
         */
        data class Capsule(
            override val style: ToastStyle = ToastStyle.NONE,
        ) : Type
    }

    /** The strategy that controls how long a toast remains visible. */
    sealed interface Perpetuation {
        /** The toast auto-dismisses after the given duration. */
        data class Ephemeral(
            val duration: Duration,
        ) : Perpetuation

        /** The toast remains on screen until the user dismisses it. */
        data object Persistent : Perpetuation
    }

    // MARK: - Companion

    companion object {
        /**
         * Presents the given toast, optionally invoking a closure
         * when the user taps it.
         *
         * If a toast identical to the one currently on screen is
         * requested, the call is silently ignored.
         *
         * @param toast The toast to present.
         * @param onTap A closure executed when the user taps the
         *   toast, or `null` for a non-interactive toast.
         */
        fun show(
            toast: Toast,
            onTap: (() -> Unit)? = null,
        ) {
            ToastPresenter.show(toast, onTap)
        }

        /** Dismisses the currently visible toast, if any. */
        fun hide() {
            ToastPresenter.hide()
        }
    }
}
