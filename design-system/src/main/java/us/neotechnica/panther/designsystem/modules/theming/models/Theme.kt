//
//  Theme.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.models

import androidx.compose.ui.graphics.Color

/**
 * The user interface style a [Theme] applies.
 *
 * [UNSPECIFIED] follows the system appearance; [LIGHT] and [DARK]
 * force the corresponding appearance regardless of the system setting.
 */
enum class ThemeStyle {
    LIGHT,
    DARK,
    UNSPECIFIED,
}

/**
 * A pair of colors representing the light and dark appearance variants
 * for a single themed color slot.
 *
 * Provide only a primary color when the same value should be used in
 * both appearances; provide a [variant] to specify a different color
 * for dark mode.
 */
data class ColorSet(
    /** The color used in light mode, or in both modes when no variant is provided. */
    val primary: Color,
    /** The color used in dark mode, or `null` to use [primary] in both. */
    val variant: Color? = null,
)

/**
 * A pairing of a semantic color slot with the colors used to fill it.
 */
data class ColoredItem(
    /** The semantic color slot this item fills. */
    val type: ColoredItemType,
    /** The colors used for this item in light and dark appearances. */
    val set: ColorSet,
)

/**
 * A named collection of colors that defines the visual appearance of
 * the app.
 *
 * A theme maps semantic color slots ([ColoredItemType]) to concrete
 * colors through a set of [ColoredItem] values. Resolve a slot with
 * [colorFor], which returns the dark-mode variant when appropriate.
 */
data class Theme(
    /** The display name of the theme. */
    val name: String,
    /** The colored items that make up this theme's palette. */
    val items: Set<ColoredItem>,
    /** The user interface style this theme applies. */
    val style: ThemeStyle = ThemeStyle.UNSPECIFIED,
) {
    // MARK: - Methods

    /**
     * Returns the color for the given item type, selecting the
     * appropriate appearance variant.
     *
     * When [isDark] is `true`, the variant color is returned if one
     * was provided; otherwise the primary color is used.
     *
     * @param itemType The semantic color slot to resolve.
     * @param isDark Whether dark mode is active.
     *
     * @return The resolved color, or [Color.Unspecified] if the slot is
     *   not present in this theme's palette.
     */
    fun colorFor(
        itemType: ColoredItemType,
        isDark: Boolean,
    ): Color {
        val set = items.firstOrNull { it.type == itemType }?.set ?: return Color.Unspecified
        return if (isDark) set.variant ?: set.primary else set.primary
    }
}
