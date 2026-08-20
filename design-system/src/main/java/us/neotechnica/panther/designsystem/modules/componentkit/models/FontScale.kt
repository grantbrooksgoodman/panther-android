//
//  FontScale.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

/**
 * The point size of a font, corresponding to standard iOS Dynamic
 * Type sizes.
 *
 * The predefined cases map to standard iOS Dynamic Type sizes:
 * [Large] is 28 points (Title 1), [Medium] is 17 points (Body), and
 * [Small] is 13 points (Footnote). Use [Custom] for an arbitrary size.
 */
sealed interface FontScale {
    // MARK: - Properties

    /** The size of the font in points. */
    val points: Float

    // MARK: - Cases

    /** A font size of 28 points, corresponding to Title 1. */
    data object Large : FontScale {
        override val points: Float get() = LARGE_POINTS
    }

    /** A font size of 17 points, corresponding to Body. */
    data object Medium : FontScale {
        override val points: Float get() = MEDIUM_POINTS
    }

    /** A font size of 13 points, corresponding to Footnote. */
    data object Small : FontScale {
        override val points: Float get() = SMALL_POINTS
    }

    /** A custom point size. */
    data class Custom(
        override val points: Float,
    ) : FontScale

    // MARK: - Companion

    companion object {
        private const val LARGE_POINTS = 28f
        private const val MEDIUM_POINTS = 17f
        private const val SMALL_POINTS = 13f
    }
}
