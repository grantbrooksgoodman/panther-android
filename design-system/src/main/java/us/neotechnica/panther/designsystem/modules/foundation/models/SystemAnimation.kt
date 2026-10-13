//
//  SystemAnimation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.models

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import us.neotechnica.panther.designsystem.modules.foundation.constants.SystemAnimationFloats
import kotlin.math.PI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias Floats = SystemAnimationFloats

/**
 * The animations of iOS's system transitions and SwiftUI's animation
 * presets, for matching iOS motion.
 *
 * Use these in place of Material's motion wherever the iOS app relies
 * on a system default – a navigation push, a sheet, an alert – or on
 * a SwiftUI animation preset:
 *
 * ```kotlin
 * val alpha by animateFloatAsState(
 *     targetValue = if (isVisible) 1f else 0f,
 *     animationSpec = SystemAnimation.easeIn(),
 * )
 * ```
 */
object SystemAnimation {
    // MARK: - UIKit

    /**
     * The spring of an alert or action sheet as it presents or
     * dismisses.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> alertTransition(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(Spring.DampingRatioNoBouncy, Floats.ALERT_TRANSITION_SPRING_STIFFNESS, visibilityThreshold)

    /**
     * The spring of a navigation push or pop.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> navigationTransition(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(Spring.DampingRatioNoBouncy, Floats.NAVIGATION_TRANSITION_SPRING_STIFFNESS, visibilityThreshold)

    /**
     * The spring of a sheet as it presents, dismisses, or settles after
     * a drag. It is the navigation transition's spring.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> sheetTransition(visibilityThreshold: T? = null): SpringSpec<T> = navigationTransition(visibilityThreshold)

    // MARK: - SwiftUI

    /**
     * SwiftUI's `easeIn` animation.
     *
     * @param duration The animation's duration. The default is 0.35
     *   seconds.
     */
    fun <T> easeIn(duration: Duration = Floats.DEFAULT_TIMING_CURVE_DURATION_MILLISECONDS.milliseconds): TweenSpec<T> =
        tween(duration.inWholeMilliseconds.toInt(), easing = EaseIn)

    /**
     * SwiftUI's `easeInOut` animation – also the curve of a UIKit
     * `UIView.animate(withDuration:)` animation.
     *
     * @param duration The animation's duration. The default is 0.35
     *   seconds.
     */
    fun <T> easeInOut(duration: Duration = Floats.DEFAULT_TIMING_CURVE_DURATION_MILLISECONDS.milliseconds): TweenSpec<T> =
        tween(duration.inWholeMilliseconds.toInt(), easing = EaseInOut)

    /**
     * SwiftUI's `easeOut` animation.
     *
     * @param duration The animation's duration. The default is 0.35
     *   seconds.
     */
    fun <T> easeOut(duration: Duration = Floats.DEFAULT_TIMING_CURVE_DURATION_MILLISECONDS.milliseconds): TweenSpec<T> =
        tween(duration.inWholeMilliseconds.toInt(), easing = EaseOut)

    /**
     * SwiftUI's default `spring()` animation.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> spring(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(Spring.DampingRatioNoBouncy, springStiffness(Floats.SPRING_RESPONSE), visibilityThreshold)

    // MARK: - Auxiliary

    // The unit-mass stiffness of a SwiftUI spring with the given response,
    // in seconds: the square of its angular frequency.
    private fun springStiffness(response: Float): Float {
        val angularFrequency = 2 * PI.toFloat() / response
        return angularFrequency * angularFrequency
    }
}
