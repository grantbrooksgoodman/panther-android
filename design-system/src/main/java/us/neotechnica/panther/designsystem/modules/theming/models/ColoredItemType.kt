//
//  ColoredItemType.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.models

/**
 * An identifier for a semantic color slot within a [Theme].
 *
 * Colored item types act as keys that map a design concept – such as
 * "accent" or "background" – to a concrete color in the active theme.
 * The framework provides a set of built-in types; declare additional
 * app-specific types as companion constants.
 */
@JvmInline
value class ColoredItemType(
    /** The string value that uniquely identifies this color slot. */
    val rawValue: String,
) {
    // MARK: - Companion

    companion object {
        val accent = ColoredItemType("accent")
        val background = ColoredItemType("background")
        val disabled = ColoredItemType("disabled")
        val groupedContentBackground = ColoredItemType("groupedContentBackground")
        val navigationBarBackground = ColoredItemType("navigationBarBackground")
        val navigationBarButton = ColoredItemType("navigationBarButton")
        val navigationBarTitle = ColoredItemType("navigationBarTitle")
        val reactionButtonBackground = ColoredItemType("reactionButtonBackground")
        val receiverBubble = ColoredItemType("receiverBubble")
        val senderBubble = ColoredItemType("senderBubble")
        val subtitleText = ColoredItemType("subtitleText")
        val titleText = ColoredItemType("titleText")
    }
}
