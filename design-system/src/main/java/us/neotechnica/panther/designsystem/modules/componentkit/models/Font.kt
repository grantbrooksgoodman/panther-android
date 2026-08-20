//
//  Font.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle as ComposeFontStyle

/**
 * A value that describes a font's type and scale.
 *
 * Combine a [FontType] with a [FontScale] to define the font's style
 * and point size, then convert it to a Compose [TextStyle] through
 * [textStyle]. For common configurations, use the companion factories
 * such as [systemBold] or [systemSemibold].
 */
class Font(
    /** The type of font, defining its weight and style. */
    val type: FontType,
    /** The scale of the font, defining its point size. */
    val scale: FontScale,
) {
    // MARK: - Computed Properties

    /**
     * The Compose text style for this font. Custom typefaces fall back
     * to Inter until additional fonts are shipped.
     */
    val textStyle: TextStyle
        get() =
            TextStyle(
                fontFamily = InterFontFamily,
                fontSize = scale.points.sp,
                fontWeight = weight,
                fontStyle = if (isItalic) ComposeFontStyle.Italic else ComposeFontStyle.Normal,
                textDecoration = if (isUnderlined) TextDecoration.Underline else null,
            )

    private val isItalic: Boolean
        get() =
            when (type) {
                is FontType.Custom -> type.isItalic
                is FontType.System -> type.style.isItalic
            }

    private val isUnderlined: Boolean
        get() =
            when (type) {
                is FontType.Custom -> type.isUnderlined
                is FontType.System -> type.style.isUnderlined
            }

    private val weight: FontWeight
        get() =
            when (type) {
                is FontType.Custom -> FontWeight.Normal
                is FontType.System -> type.style.weight
            }

    // MARK: - Companion

    companion object {
        /** A body-sized regular system font. */
        val system = Font(FontType.System(FontStyle.Regular()), FontScale.Medium)

        fun system(scale: FontScale) = Font(FontType.System(FontStyle.Regular()), scale)

        fun systemBold(scale: FontScale = FontScale.Medium) = Font(FontType.System(FontStyle.Bold()), scale)

        fun systemItalic(scale: FontScale = FontScale.Medium) = Font(FontType.System(FontStyle.Regular(isItalic = true)), scale)

        fun systemLight(scale: FontScale = FontScale.Medium) = Font(FontType.System(FontStyle.Light()), scale)

        fun systemMedium(scale: FontScale = FontScale.Medium) = Font(FontType.System(FontStyle.Medium()), scale)

        fun systemSemibold(scale: FontScale = FontScale.Medium) = Font(FontType.System(FontStyle.Semibold()), scale)

        fun systemUnderlined(scale: FontScale = FontScale.Medium) = Font(FontType.System(FontStyle.Regular(isUnderlined = true)), scale)
    }
}
