//
//  SystemAnimation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.models

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
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
 *
 * Each animation follows [usesAppleAnimations]: while it is `false`,
 * every animation is replaced by its Android counterpart.
 */
object SystemAnimation {
    // MARK: - Properties

    /**
     * A Boolean value that indicates whether animations match iOS's
     * timing and style.
     *
     * While this is `false`, each animation in this namespace is
     * replaced by its Android counterpart – Compose's default curve
     * and spring, and Material's alert, navigation, and bottom sheet
     * motion – and components that otherwise recreate iOS motion,
     * such as navigation stacks, page sheets, and context menus, move
     * and dim as Android does. The default is `true`.
     *
     * **Important:** Set this value once at launch, before any content
     * is composed. Animations already under way, and content already
     * on screen, don't update when it changes.
     */
    var usesAppleAnimations = true

    // A fade that runs its course over the first part of the eased
    // transition.
    private val searchFadeOutEasing =
        Easing { fraction ->
            (Ease.transform(fraction) / Floats.SEARCH_TRANSITION_FADE_FRACTION).coerceAtMost(1f)
        }

    // A fade that runs its course over the last part of the eased
    // transition.
    private val searchFadeInEasing =
        Easing { fraction ->
            val fadeStart = 1f - Floats.SEARCH_TRANSITION_FADE_FRACTION
            ((Ease.transform(fraction) - fadeStart) / Floats.SEARCH_TRANSITION_FADE_FRACTION).coerceAtLeast(0f)
        }

    // MARK: - UIKit

    /**
     * The animation of an alert or action sheet as it dismisses.
     *
     * The animation is UIKit's alert spring – the same spring with
     * which the alert or action sheet presents. While
     * [usesAppleAnimations] is `false`, it is instead Material's
     * accelerating exit curve.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> alertDismissal(visibilityThreshold: T? = null): FiniteAnimationSpec<T> {
        if (!usesAppleAnimations) return tween(Floats.MATERIAL_ALERT_DISMISSAL_DURATION_MILLISECONDS, easing = FastOutLinearInEasing)
        return alertSpring(visibilityThreshold)
    }

    /**
     * The animation of an alert or action sheet as it presents.
     *
     * The animation is UIKit's alert spring – the same spring with
     * which the alert or action sheet dismisses. While
     * [usesAppleAnimations] is `false`, it is instead Material's
     * decelerating entrance curve.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> alertPresentation(visibilityThreshold: T? = null): FiniteAnimationSpec<T> {
        if (!usesAppleAnimations) return tween(Floats.MATERIAL_ALERT_PRESENTATION_DURATION_MILLISECONDS, easing = LinearOutSlowInEasing)
        return alertSpring(visibilityThreshold)
    }

    /**
     * The animation of a navigation push or pop.
     *
     * The animation is UIKit's navigation spring. While
     * [usesAppleAnimations] is `false`, it is instead a short slide on
     * Compose's default curve.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> navigationTransition(visibilityThreshold: T? = null): FiniteAnimationSpec<T> {
        if (!usesAppleAnimations) return tween(Floats.MATERIAL_NAVIGATION_TRANSITION_DURATION_MILLISECONDS, easing = FastOutSlowInEasing)
        return navigationSpring(visibilityThreshold)
    }

    /**
     * The animation of a search bar in the navigation bar as a search
     * begins or ends.
     *
     * As a search begins, the navigation bar collapses around the
     * search bar, and the search bar's Cancel button slides in from
     * the trailing edge as its field narrows. As the search ends, the
     * motion reverses. Use [searchTransitionFade] for the navigation
     * bar's title and items over the same transition. While
     * [usesAppleAnimations] is `false`, the animation runs on Compose's
     * default curve.
     */
    fun <T> searchTransition(): TweenSpec<T> {
        if (!usesAppleAnimations) return defaultTween(duration = null)
        return tween(Floats.SEARCH_TRANSITION_DURATION_MILLISECONDS, easing = EaseInOut)
    }

    /**
     * The fade of the navigation bar's title and items as a search in
     * the navigation bar begins or ends.
     *
     * The title and items finish fading out early in a beginning
     * search's transition, and begin fading in late in an ending
     * search's transition, so that they are hidden whenever the search
     * bar passes over them. While [usesAppleAnimations] is `false`, they
     * fade over the whole transition on Compose's default curve.
     *
     * @param isBeginning A Boolean value that indicates whether the
     *   search is beginning, fading the title and items out, rather
     *   than ending, fading them in.
     */
    fun <T> searchTransitionFade(isBeginning: Boolean): TweenSpec<T> {
        if (!usesAppleAnimations) return defaultTween(duration = null)
        return tween(
            Floats.SEARCH_TRANSITION_DURATION_MILLISECONDS,
            easing = if (isBeginning) searchFadeOutEasing else searchFadeInEasing,
        )
    }

    /**
     * The animation of a sheet as it presents, dismisses, or settles
     * after a drag.
     *
     * The animation is UIKit's sheet spring, which is the navigation
     * transition's spring. While [usesAppleAnimations] is `false`, it is
     * instead Material's bottom sheet spring.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> sheetTransition(visibilityThreshold: T? = null): SpringSpec<T> {
        if (!usesAppleAnimations) {
            return spring(Floats.MATERIAL_SHEET_SPRING_DAMPING_RATIO, Floats.MATERIAL_SHEET_SPRING_STIFFNESS, visibilityThreshold)
        }

        return navigationSpring(visibilityThreshold)
    }

    // MARK: - SwiftUI

    /**
     * SwiftUI's `easeIn` animation.
     *
     * While [usesAppleAnimations] is `false`, the animation runs on
     * Compose's default curve instead.
     *
     * @param duration The animation's duration, or `null` for the
     *   default: 0.35 seconds, or Compose's default duration while
     *   [usesAppleAnimations] is `false`.
     */
    fun <T> easeIn(duration: Duration? = null): TweenSpec<T> = timingCurve(EaseIn, duration)

    /**
     * SwiftUI's `easeInOut` animation – also the curve of a UIKit
     * `UIView.animate(withDuration:)` animation.
     *
     * While [usesAppleAnimations] is `false`, the animation runs on
     * Compose's default curve instead.
     *
     * @param duration The animation's duration, or `null` for the
     *   default: 0.35 seconds, or Compose's default duration while
     *   [usesAppleAnimations] is `false`.
     */
    fun <T> easeInOut(duration: Duration? = null): TweenSpec<T> = timingCurve(EaseInOut, duration)

    /**
     * SwiftUI's `easeOut` animation.
     *
     * While [usesAppleAnimations] is `false`, the animation runs on
     * Compose's default curve instead.
     *
     * @param duration The animation's duration, or `null` for the
     *   default: 0.35 seconds, or Compose's default duration while
     *   [usesAppleAnimations] is `false`.
     */
    fun <T> easeOut(duration: Duration? = null): TweenSpec<T> = timingCurve(EaseOut, duration)

    /**
     * SwiftUI's default `spring()` animation.
     *
     * While [usesAppleAnimations] is `false`, the animation is instead
     * the spring Compose gives an enter or exit transition by default.
     *
     * @param visibilityThreshold The distance from the target at which
     *   the animation is considered settled, or `null` for the default.
     */
    fun <T> spring(visibilityThreshold: T? = null): SpringSpec<T> {
        if (!usesAppleAnimations) return spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow, visibilityThreshold)
        return spring(Spring.DampingRatioNoBouncy, springStiffness(Floats.SPRING_RESPONSE), visibilityThreshold)
    }

    // MARK: - Auxiliary

    private fun <T> alertSpring(visibilityThreshold: T?): SpringSpec<T> =
        spring(Spring.DampingRatioNoBouncy, Floats.ALERT_TRANSITION_SPRING_STIFFNESS, visibilityThreshold)

    // A tween on Compose's default curve, lasting the given duration or
    // Compose's default duration.
    private fun <T> defaultTween(duration: Duration?): TweenSpec<T> =
        tween(
            duration?.inWholeMilliseconds?.toInt() ?: AnimationConstants.DefaultDurationMillis,
            easing = FastOutSlowInEasing,
        )

    private fun <T> navigationSpring(visibilityThreshold: T?): SpringSpec<T> =
        spring(Spring.DampingRatioNoBouncy, Floats.NAVIGATION_TRANSITION_SPRING_STIFFNESS, visibilityThreshold)

    // The unit-mass stiffness of a SwiftUI spring with the given response,
    // in seconds: the square of its angular frequency.
    private fun springStiffness(response: Float): Float {
        val angularFrequency = 2 * PI.toFloat() / response
        return angularFrequency * angularFrequency
    }

    // A tween on the given SwiftUI curve, or on Compose's default curve
    // while iOS animations are disabled.
    private fun <T> timingCurve(
        easing: Easing,
        duration: Duration?,
    ): TweenSpec<T> {
        if (!usesAppleAnimations) return defaultTween(duration)
        val iOSDuration = duration ?: Floats.DEFAULT_TIMING_CURVE_DURATION_MILLISECONDS.milliseconds
        return tween(iOSDuration.inWholeMilliseconds.toInt(), easing = easing)
    }
}
