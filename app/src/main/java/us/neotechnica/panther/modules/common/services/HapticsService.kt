//
//  HapticsService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import android.view.HapticFeedbackConstants
import us.neotechnica.panther.translator.Translator

/**
 * Plays haptic feedback.
 *
 * Each style maps to the closest [HapticFeedbackConstants]
 * value played on the current activity's view (see [generateFeedback]).
 */
object HapticsService {
    // MARK: - Types

    /** The kind of haptic feedback to play. */
    enum class HapticFeedbackStyle {
        /** Impact feedback between large, heavy interface elements. */
        HEAVY,

        /** Impact feedback between small, light interface elements. */
        LIGHT,

        /** Impact feedback between moderately sized interface elements. */
        MEDIUM,

        /** Impact feedback between hard or inflexible interface elements. */
        RIGID,

        /** Feedback indicating a change in selection. */
        SELECTION,

        /** Impact feedback between soft or flexible interface elements. */
        SOFT,
    }

    // MARK: - Methods

    /**
     * Plays haptic feedback of the given style on the current
     * activity's view, or does nothing if no activity is foregrounded.
     *
     * @param style The kind of haptic feedback to play.
     */
    fun generateFeedback(style: HapticFeedbackStyle) {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return
        val view = activity.window?.decorView ?: return
        view.performHapticFeedback(hapticFeedbackConstant(style))
    }

    /**
     * Prepares the given style's generator to receive events.
     *
     * A no-op: `View.performHapticFeedback` has no pre-warm step, and
     * it is retained for signature compatibility.
     *
     * @param generatorStyle The style of generator to prepare.
     */
    @Suppress("UnusedParameter")
    fun prepare(generatorStyle: HapticFeedbackStyle) = Unit

    // MARK: - Auxiliary

    private fun hapticFeedbackConstant(style: HapticFeedbackStyle): Int =
        when (style) {
            HapticFeedbackStyle.HEAVY -> HapticFeedbackConstants.LONG_PRESS
            HapticFeedbackStyle.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticFeedbackStyle.MEDIUM -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticFeedbackStyle.RIGID -> HapticFeedbackConstants.CLOCK_TICK
            HapticFeedbackStyle.SELECTION -> HapticFeedbackConstants.CLOCK_TICK
            HapticFeedbackStyle.SOFT -> HapticFeedbackConstants.VIRTUAL_KEY
        }
}
