//
//  HeaderView+FoundationExtensions.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.foundation.components.HeaderView
import us.neotechnica.panther.designsystem.modules.foundation.constants.HeaderViewFloats
import us.neotechnica.panther.designsystem.modules.foundation.constants.HeaderViewStrings
import us.neotechnica.panther.subsystem.modules.localization.models.Localized
import us.neotechnica.panther.subsystem.modules.localization.models.SubsystemStringKey

/**
 * Returns a back button showing the standard back chevron.
 *
 * @param foregroundColor The chevron's tint, or `null` for the
 *   theme's accent color.
 * @param isEnabled Whether the button responds to taps.
 * @param action The action to perform when tapped.
 *
 * @return The back button.
 */
fun HeaderView.PeripheralButtonType.Companion.backButton(
    foregroundColor: Color? = null,
    isEnabled: Boolean = true,
    action: () -> Unit,
): HeaderView.PeripheralButtonType =
    HeaderView.PeripheralButtonType.Image(
        HeaderView.ImageButtonAttributes(
            image =
                HeaderView.ImageAttributes(
                    systemName = HeaderViewStrings.BACK_BUTTON_IMAGE_SYSTEM_NAME,
                    foregroundColor = foregroundColor,
                    size =
                        DpSize(
                            HeaderViewFloats.BACK_BUTTON_IMAGE_SIZE_WIDTH.dp,
                            HeaderViewFloats.BACK_BUTTON_IMAGE_SIZE_HEIGHT.dp,
                        ),
                ),
            isEnabled = isEnabled,
            action = action,
        ),
    )

/**
 * Returns a cancel button showing the localized cancel string.
 *
 * @param font The label's font. The default is the 17-point system
 *   font.
 * @param foregroundColor The label's color, or `null` for the
 *   theme's accent color.
 * @param isEnabled Whether the button responds to taps.
 * @param action The action to perform when tapped.
 *
 * @return The cancel button.
 */
fun HeaderView.PeripheralButtonType.Companion.cancelButton(
    font: Font = Font.system(FontScale.Custom(PERIPHERAL_BUTTON_FONT_SIZE)),
    foregroundColor: Color? = null,
    isEnabled: Boolean = true,
    action: () -> Unit,
): HeaderView.PeripheralButtonType =
    HeaderView.PeripheralButtonType.Text(
        HeaderView.TextButtonAttributes(
            text =
                HeaderView.TextAttributes(
                    Localized(SubsystemStringKey.CANCEL).wrappedValue,
                    font = font,
                    foregroundColor = foregroundColor,
                ),
            isEnabled = isEnabled,
            action = action,
        ),
    )

/**
 * Returns a done button showing the localized done string.
 *
 * @param font The label's font. The default is the 17-point
 *   semibold system font.
 * @param foregroundColor The label's color, or `null` for the
 *   theme's accent color.
 * @param isEnabled Whether the button responds to taps.
 * @param action The action to perform when tapped.
 *
 * @return The done button.
 */
fun HeaderView.PeripheralButtonType.Companion.doneButton(
    font: Font = Font.systemSemibold(FontScale.Custom(PERIPHERAL_BUTTON_FONT_SIZE)),
    foregroundColor: Color? = null,
    isEnabled: Boolean = true,
    action: () -> Unit,
): HeaderView.PeripheralButtonType =
    HeaderView.PeripheralButtonType.Text(
        HeaderView.TextButtonAttributes(
            text =
                HeaderView.TextAttributes(
                    Localized(SubsystemStringKey.DONE).wrappedValue,
                    font = font,
                    foregroundColor = foregroundColor,
                ),
            isEnabled = isEnabled,
            action = action,
        ),
    )

private const val PERIPHERAL_BUTTON_FONT_SIZE = 17f
