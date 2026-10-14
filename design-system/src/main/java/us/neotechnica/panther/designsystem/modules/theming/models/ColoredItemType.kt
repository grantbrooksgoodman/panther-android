//
//  ColoredItemType.kt
//  Panther Android
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
        /** The accent color slot. */
        val accent = ColoredItemType("accent")

        /** The primary background color slot. */
        val background = ColoredItemType("background")

        /** The color slot of disabled content. */
        val disabled = ColoredItemType("disabled")

        /** The background color slot behind grouped lists. */
        val groupedContentBackground = ColoredItemType("groupedContentBackground")

        /** The background color slot of a grouped list row. */
        val groupedRowBackground = ColoredItemType("groupedRowBackground")

        /** The navigation bar's background color slot. */
        val navigationBarBackground = ColoredItemType("navigationBarBackground")

        /** The navigation bar's button color slot. */
        val navigationBarButton = ColoredItemType("navigationBarButton")

        /** The navigation bar's title color slot. */
        val navigationBarTitle = ColoredItemType("navigationBarTitle")

        /** The background color slot of a reaction chip. */
        val reactionButtonBackground = ColoredItemType("reactionButtonBackground")

        /** The color slot of a received message bubble. */
        val receiverBubble = ColoredItemType("receiverBubble")

        /** The color slot of a sent message bubble. */
        val senderBubble = ColoredItemType("senderBubble")

        /** The secondary text color slot. */
        val subtitleText = ColoredItemType("subtitleText")

        /** The primary text color slot. */
        val titleText = ColoredItemType("titleText")
    }
}
