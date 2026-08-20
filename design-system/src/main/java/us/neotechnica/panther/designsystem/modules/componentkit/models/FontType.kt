//
//  FontType.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

/**
 * The type of a font, distinguishing between custom and system
 * typefaces.
 */
sealed interface FontType {
    // MARK: - Cases

    /**
     * A custom typeface identified by its registered font name.
     *
     * **Note:** Named custom typefaces beyond the bundled Inter family
     * fall back to Inter until additional fonts are shipped.
     */
    data class Custom(
        val name: String,
        val isItalic: Boolean = false,
        val isUnderlined: Boolean = false,
    ) : FontType

    /** The system typeface (Inter on Android). */
    data class System(
        val style: FontStyle,
    ) : FontType
}
