//
//  FontStyle.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.font.FontWeight

/**
 * The weight and style attributes of a system font.
 *
 * Each case corresponds to a standard font weight and accepts optional
 * `isItalic` and `isUnderlined` flags, both defaulting to `false`. Use
 * [Custom] to specify an arbitrary [FontWeight].
 */
sealed interface FontStyle {
    // MARK: - Properties

    /** Whether the font renders in italic. */
    val isItalic: Boolean

    /** Whether the text renders with an underline. */
    val isUnderlined: Boolean

    /** The Compose weight for this style. */
    val weight: FontWeight

    // MARK: - Cases

    data class Bold(
        override val isItalic: Boolean = false,
        override val isUnderlined: Boolean = false,
    ) : FontStyle {
        override val weight: FontWeight get() = FontWeight.Bold
    }

    data class Custom(
        override val weight: FontWeight,
        override val isItalic: Boolean = false,
        override val isUnderlined: Boolean = false,
    ) : FontStyle

    data class Light(
        override val isItalic: Boolean = false,
        override val isUnderlined: Boolean = false,
    ) : FontStyle {
        override val weight: FontWeight get() = FontWeight.Light
    }

    data class Medium(
        override val isItalic: Boolean = false,
        override val isUnderlined: Boolean = false,
    ) : FontStyle {
        override val weight: FontWeight get() = FontWeight.Medium
    }

    data class Regular(
        override val isItalic: Boolean = false,
        override val isUnderlined: Boolean = false,
    ) : FontStyle {
        override val weight: FontWeight get() = FontWeight.Normal
    }

    data class Semibold(
        override val isItalic: Boolean = false,
        override val isUnderlined: Boolean = false,
    ) : FontStyle {
        override val weight: FontWeight get() = FontWeight.SemiBold
    }
}
