//
//  FontType.kt
//  Panther Android
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
     * **Note:** Text renders in [SystemFontFamily] regardless of the
     * name; a custom type contributes only its italic and underline
     * attributes.
     */
    data class Custom(
        val name: String,
        val isItalic: Boolean = false,
        val isUnderlined: Boolean = false,
    ) : FontType

    /** The system typeface, in [SystemFontFamily]. */
    data class System(
        val style: FontStyle,
    ) : FontType
}
