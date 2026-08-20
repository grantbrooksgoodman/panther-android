//
//  PantherColors.kt
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

    val accent: Color get() = color(ColoredItemType.accent)
    val background: Color get() = color(ColoredItemType.background)
    val disabled: Color get() = color(ColoredItemType.disabled)
    val groupedContentBackground: Color get() = color(ColoredItemType.groupedContentBackground)
    val navigationBarBackground: Color get() = color(ColoredItemType.navigationBarBackground)
    val navigationBarButton: Color get() = color(ColoredItemType.navigationBarButton)
    val navigationBarTitle: Color get() = color(ColoredItemType.navigationBarTitle)
    val receiverBubble: Color get() = color(ColoredItemType.receiverBubble)
    val senderBubble: Color get() = color(ColoredItemType.senderBubble)
    val subtitleText: Color get() = color(ColoredItemType.subtitleText)
    val titleText: Color get() = color(ColoredItemType.titleText)

    // MARK: - Methods

    /** Returns the resolved color for the given semantic slot. */
    fun color(itemType: ColoredItemType): Color = theme.colorFor(itemType, isDark)
}
