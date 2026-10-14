//
//  FoundationConstants+SystemAnimation.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// MARK: - Float

/**
 * The parameters of iOS's system animations, measured from the Core
 * Animation animations UIKit and SwiftUI attach on iOS 18, and of the
 * Material animations that replace them while iOS animations are
 * disabled.
 *
 * Every UIKit spring below is critically damped. Core Animation
 * clamps an overdamped spring to critical damping, so the navigation
 * spring – mass 3, stiffness 1000, damping 500 – moves as a critically
 * damped spring with a stiffness of 1000 / 3 for unit mass.
 */
object SystemAnimationFloats {
    /** The stiffness of the spring of an alert or action sheet presentation. */
    const val ALERT_TRANSITION_SPRING_STIFFNESS = 522.35f

    /** The duration of SwiftUI's ease-in, ease-out, and ease-in-out animations, in milliseconds. */
    const val DEFAULT_TIMING_CURVE_DURATION_MILLISECONDS = 350

    /** The duration, in milliseconds, of Material's alert or action sheet dismissal. */
    const val MATERIAL_ALERT_DISMISSAL_DURATION_MILLISECONDS = 250

    /** The duration, in milliseconds, of Material's alert or action sheet presentation. */
    const val MATERIAL_ALERT_PRESENTATION_DURATION_MILLISECONDS = 300

    /** The duration, in milliseconds, of a navigation push or pop on Android. */
    const val MATERIAL_NAVIGATION_TRANSITION_DURATION_MILLISECONDS = 250

    /** The opacity of Material's scrim behind a bottom sheet. */
    const val MATERIAL_SHEET_SCRIM_ALPHA = 0.32f

    /** The damping ratio of Material's bottom sheet spring. */
    const val MATERIAL_SHEET_SPRING_DAMPING_RATIO = 0.9f

    /** The stiffness of Material's bottom sheet spring. */
    const val MATERIAL_SHEET_SPRING_STIFFNESS = 700f

    /** The opacity of the black dimming over the page beneath a navigation transition. */
    const val NAVIGATION_DIMMING_ALPHA = 0.1f

    /** The opacity of the navigation edge shadow at its edge farthest from the page. */
    const val NAVIGATION_EDGE_SHADOW_OUTER_ALPHA = 0.047f

    /** The opacity of the navigation edge shadow at the page's edge. */
    const val NAVIGATION_EDGE_SHADOW_INNER_ALPHA = 0.071f

    /** The width of the shadow cast along the leading edge of a page sliding over another. */
    val navigationEdgeShadowWidth: Dp = 9.dp

    /** The fraction of its width by which the page beneath a navigation transition slides. */
    const val NAVIGATION_PARALLAX_FRACTION = 0.3f

    /** The stiffness of the spring of a navigation push or pop and of a sheet presentation. */
    const val NAVIGATION_TRANSITION_SPRING_STIFFNESS = 1000f / 3f

    /** The duration, in milliseconds, of a search transition in the navigation bar. */
    const val SEARCH_TRANSITION_DURATION_MILLISECONDS = 300

    /**
     * The fraction of a search transition's eased progress over which
     * the navigation bar's title and items fade.
     */
    const val SEARCH_TRANSITION_FADE_FRACTION = 0.35f

    /** The opacity of the black dimming behind a presented sheet. */
    const val SHEET_DIMMING_ALPHA = 0.12f

    /** The response, in seconds, of SwiftUI's default spring. */
    const val SPRING_RESPONSE = 0.5f
}
