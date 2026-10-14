//
//  PantherColors.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.views

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import us.neotechnica.panther.designsystem.modules.theming.models.ColoredItemType
import us.neotechnica.panther.designsystem.modules.theming.models.Theme

/**
 * The resolved color palette for the active theme and appearance.
 *
 * Read individual slots through the named accessors or the generic
 * [color] method. Obtain the current palette in a composable through
 * [LocalPantherColors].
 */
@Immutable
class PantherColors(
    private val theme: Theme,
    private val isDark: Boolean,
) {
    // MARK: - Computed Properties

    /** The accent color. */
    val accent: Color get() = color(ColoredItemType.accent)

    /** The primary background color. */
    val background: Color get() = color(ColoredItemType.background)

    /** The color of disabled content. */
    val disabled: Color get() = color(ColoredItemType.disabled)

    /** The background color behind grouped lists. */
    val groupedContentBackground: Color get() = color(ColoredItemType.groupedContentBackground)

    /** The background color of a grouped list row. */
    val groupedRowBackground: Color get() = color(ColoredItemType.groupedRowBackground)

    /** The navigation bar's background color. */
    val navigationBarBackground: Color get() = color(ColoredItemType.navigationBarBackground)

    /** The navigation bar's button color. */
    val navigationBarButton: Color get() = color(ColoredItemType.navigationBarButton)

    /** The navigation bar's title color. */
    val navigationBarTitle: Color get() = color(ColoredItemType.navigationBarTitle)

    /** The background color of a reaction chip. */
    val reactionButtonBackground: Color get() = color(ColoredItemType.reactionButtonBackground)

    /** The color of a received message bubble. */
    val receiverBubble: Color get() = color(ColoredItemType.receiverBubble)

    /** The color of a sent message bubble. */
    val senderBubble: Color get() = color(ColoredItemType.senderBubble)

    /** The secondary text color. */
    val subtitleText: Color get() = color(ColoredItemType.subtitleText)

    /** The primary text color. */
    val titleText: Color get() = color(ColoredItemType.titleText)

    // MARK: - Methods

    /** Returns the resolved color for the given semantic slot. */
    fun color(itemType: ColoredItemType): Color = theme.colorFor(itemType, isDark)
}
