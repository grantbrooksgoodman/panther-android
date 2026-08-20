//
//  Themes.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.theming.models

import androidx.compose.ui.graphics.Color

/**
 * The app's built-in themes, ported from the iOS `UIThemes`
 * definitions.
 *
 * **Note:** the iOS `prevaricationMode` theme is omitted, as
 * prevarication mode is cut from the MVP.
 */
object Themes {
    // MARK: - Themes

    /** The default theme, following the system appearance. */
    val appDefault =
        Theme(
            name = "Default",
            items =
                setOf(
                    item(ColoredItemType.accent, PlatformColors.systemBlue),
                    item(ColoredItemType.background, PlatformColors.white, PlatformColors.black),
                    item(ColoredItemType.disabled, PlatformColors.systemGray3),
                    item(ColoredItemType.groupedContentBackground, rgb(0xF2F2F7), rgb(0x1C1C1E)),
                    item(ColoredItemType.navigationBarBackground, rgb(0xF8F8F8), rgb(0x2A2A2C)),
                    item(ColoredItemType.navigationBarButton, PlatformColors.systemBlue),
                    item(ColoredItemType.navigationBarTitle, PlatformColors.black, PlatformColors.white),
                    item(ColoredItemType.senderBubble, PlatformColors.systemBlue),
                    item(ColoredItemType.receiverBubble, rgb(0xE5E5EA), rgb(0x27252A)),
                    item(ColoredItemType.titleText, PlatformColors.black, PlatformColors.white),
                    item(ColoredItemType.subtitleText, PlatformColors.gray),
                ),
        )

    /** A dark blue theme. */
    val bluesky = darkTheme("Bluesky", rgb(0x30AAF2))

    /** A dark orange theme. */
    val dusk = darkTheme("Dusk", rgb(0xFA8231))

    /** A dark red theme. */
    val firebrand = darkTheme("Firebrand", rgb(0xFF5252))

    /** A dark purple theme. */
    val twilight = darkTheme("Twilight", rgb(0x786DC4))

    /** Every built-in theme, in display order. */
    val all: List<Theme> = listOf(appDefault, bluesky, dusk, firebrand, twilight)

    // MARK: - Auxiliary

    private fun item(
        type: ColoredItemType,
        primary: Color,
        variant: Color? = null,
    ): ColoredItem = ColoredItem(type, ColorSet(primary, variant))

    // The dark themes share one structure, differing only by accent.
    private fun darkTheme(
        name: String,
        accent: Color,
    ): Theme =
        Theme(
            name = name,
            items =
                setOf(
                    item(ColoredItemType.accent, accent),
                    item(ColoredItemType.background, darkThemeBackground),
                    item(ColoredItemType.disabled, PlatformColors.systemGray3),
                    item(ColoredItemType.groupedContentBackground, darkThemeGroupedBackground),
                    item(ColoredItemType.navigationBarBackground, darkThemeBackground),
                    item(ColoredItemType.navigationBarButton, accent),
                    item(ColoredItemType.navigationBarTitle, PlatformColors.white),
                    item(ColoredItemType.senderBubble, accent),
                    item(ColoredItemType.receiverBubble, darkThemeReceiverBubble),
                    item(ColoredItemType.titleText, PlatformColors.white),
                    item(ColoredItemType.subtitleText, PlatformColors.lightGray),
                ),
            style = ThemeStyle.DARK,
        )
}

// The shared dark-theme greys, hoisted so they read as design tokens.
private val darkThemeBackground = rgb(0x1A1A1A)
private val darkThemeGroupedBackground = rgb(0x1C1C1E)
private val darkThemeReceiverBubble = rgb(0x27252A)
