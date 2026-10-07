//
//  HeaderView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.TextFit
import us.neotechnica.panther.designsystem.modules.foundation.constants.HeaderViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.HeaderViewFloats
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Constants Accessors

private typealias HeaderColors = HeaderViewColors
private typealias HeaderFloats = HeaderViewFloats

/**
 * A page header: optional left and right buttons around an optional
 * centered title, image, or title-and-subtitle pair, above a hairline
 * divider.
 *
 * Build peripheral buttons with the
 * [backButton][us.neotechnica.panther.designsystem.modules.foundation.extensions.backButton],
 * [cancelButton][us.neotechnica.panther.designsystem.modules.foundation.extensions.cancelButton],
 * and
 * [doneButton][us.neotechnica.panther.designsystem.modules.foundation.extensions.doneButton]
 * factories. Pass trailing `content` to lay the header out above a
 * page's content.
 */
object HeaderView {
    // MARK: - Types

    /** The header's background treatment. */
    sealed interface Appearance {
        /** A fixed custom background color. */
        data class Custom(
            val backgroundColor: Color,
        ) : Appearance

        /** The theme's navigation-bar background and title colors. */
        data object Themed : Appearance
    }

    /** The content displayed at the header's center. */
    sealed interface CenterItemType {
        /** A centered image. */
        data class Image(
            val attributes: ImageAttributes,
        ) : CenterItemType

        /** A centered title with an optional subtitle. */
        data class Text(
            val attributes: TextAttributes,
            val subtitle: TextAttributes? = null,
        ) : CenterItemType
    }

    /** A button at the header's left or right edge. */
    sealed interface PeripheralButtonType {
        /** An image button. */
        data class Image(
            val attributes: ImageButtonAttributes,
        ) : PeripheralButtonType

        /** A text button. */
        data class Text(
            val attributes: TextButtonAttributes,
        ) : PeripheralButtonType

        // MARK: - Companion

        companion object
    }

    /** The header's minimum height treatment. */
    sealed interface SizeClass {
        /** A custom minimum height. */
        data class Custom(
            val minHeight: Float,
        ) : SizeClass

        /** The full-screen-cover minimum height. */
        data object FullScreenCover : SizeClass

        /** The sheet minimum height. */
        data object Sheet : SizeClass
    }

    /**
     * The header's appearance, divider, and sizing attributes.
     */
    class Attributes(
        /** The background treatment. */
        val appearance: Appearance = Appearance.Themed,
        /** Whether the hairline divider shows below the header. */
        val showsDivider: Boolean = true,
        /** The minimum height treatment. */
        val sizeClass: SizeClass = SizeClass.Sheet,
    )

    /** The appearance of a header image. */
    class ImageAttributes(
        /** The SF Symbol name of the image. */
        val systemName: String,
        /** The image's tint. */
        val foregroundColor: Color? = null,
        /** The image's size, or `null` to fit its space. */
        val size: DpSize? = null,
    )

    /** The appearance of a header text item. */
    class TextAttributes(
        /** The string to display. */
        val string: String,
        /** The text's font. */
        val font: Font = Font.system,
        /** The text's color, or `null` for the themed color. */
        val foregroundColor: Color? = null,
    )

    /** An image button's content, enablement, and action. */
    class ImageButtonAttributes(
        /** The button's image. */
        val image: ImageAttributes,
        /** Whether the button responds to taps. */
        val isEnabled: Boolean = true,
        /** The action to perform when tapped. */
        val action: () -> Unit,
    )

    /** A text button's content, enablement, and action. */
    class TextButtonAttributes(
        /** The button's text. */
        val text: TextAttributes,
        /** Whether the button responds to taps. */
        val isEnabled: Boolean = true,
        /** The action to perform when tapped. */
        val action: () -> Unit,
    )

    // MARK: - Body

    @Composable
    operator fun invoke(
        leftItem: PeripheralButtonType? = null,
        centerItem: CenterItemType? = null,
        rightItem: PeripheralButtonType? = null,
        attributes: Attributes = Attributes(),
    ) {
        val colors = LocalPantherColors.current
        val backgroundColor =
            when (val appearance = attributes.appearance) {
                is Appearance.Custom -> appearance.backgroundColor
                Appearance.Themed -> colors.navigationBarBackground
            }

        Column(modifier = Modifier.background(backgroundColor)) {
            ContentView(leftItem, centerItem, rightItem, attributes)
            if (attributes.showsDivider) {
                HorizontalDivider(
                    color =
                        if (isSystemInDarkTheme()) {
                            HeaderColors.SEPARATOR_DARK_FOREGROUND
                        } else {
                            HeaderColors.SEPARATOR_LIGHT_FOREGROUND
                        },
                    thickness = HeaderFloats.SEPARATOR_MAX_HEIGHT.dp,
                )
            }
        }
    }

    /**
     * Lays the header out above the given content.
     *
     * @param leftItem The button at the left edge, or `null`.
     * @param centerItem The centered content, or `null`.
     * @param rightItem The button at the right edge, or `null`.
     * @param attributes The header's appearance attributes.
     * @param content The page content below the header.
     */
    @Composable
    operator fun invoke(
        leftItem: PeripheralButtonType? = null,
        centerItem: CenterItemType? = null,
        rightItem: PeripheralButtonType? = null,
        attributes: Attributes = Attributes(),
        content: @Composable () -> Unit,
    ) {
        Column {
            invoke(leftItem, centerItem, rightItem, attributes)
            content()
        }
    }
}

// MARK: - Content

@Composable
private fun ContentView(
    leftItem: HeaderView.PeripheralButtonType?,
    centerItem: HeaderView.CenterItemType?,
    rightItem: HeaderView.PeripheralButtonType?,
    attributes: HeaderView.Attributes,
) {
    val minHeight =
        when (val sizeClass = attributes.sizeClass) {
            is HeaderView.SizeClass.Custom -> sizeClass.minHeight
            HeaderView.SizeClass.FullScreenCover -> HeaderFloats.FULL_SCREEN_COVER_SIZE_CLASS_FRAME_MIN_HEIGHT
            HeaderView.SizeClass.Sheet -> HeaderFloats.SHEET_SIZE_CLASS_FRAME_MIN_HEIGHT
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.dp)
                .padding(horizontal = HeaderFloats.HORIZONTAL_PADDING.dp),
    ) {
        Row(modifier = Modifier.weight(1f)) {
            leftItem?.let { PeripheralButton(it, attributes) }
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (centerItem) {
                is HeaderView.CenterItemType.Image -> CenterImage(centerItem.attributes, attributes)
                is HeaderView.CenterItemType.Text -> {
                    CenterText(centerItem.attributes, attributes)
                    centerItem.subtitle?.let { CenterText(it, attributes) }
                }

                null -> Unit
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.weight(1f))
            rightItem?.let { PeripheralButton(it, attributes) }
        }
    }
}

@Composable
private fun CenterImage(
    imageAttributes: HeaderView.ImageAttributes,
    attributes: HeaderView.Attributes,
) {
    val colors = LocalPantherColors.current
    val tint =
        if (attributes.appearance == HeaderView.Appearance.Themed) {
            colors.navigationBarTitle
        } else {
            imageAttributes.foregroundColor ?: colors.navigationBarTitle
        }

    Components.Symbol(
        imageAttributes.systemName,
        foregroundColor = tint,
        usesIntrinsicSize = false,
        modifier =
            if (imageAttributes.size != null) {
                Modifier.size(imageAttributes.size)
            } else {
                Modifier.sizeIn(maxHeight = HeaderFloats.IMAGE_MAX_HEIGHT.dp)
            },
    )
}

@Composable
private fun CenterText(
    textAttributes: HeaderView.TextAttributes,
    attributes: HeaderView.Attributes,
) {
    val colors = LocalPantherColors.current
    val isLong =
        textAttributes.string.length >= HeaderFloats.LONG_CENTER_ITEM_TEXT_CHARACTER_COUNT_THRESHOLD.toInt()
    Components.FittedText(
        textAttributes.string,
        foregroundColor =
            if (attributes.appearance == HeaderView.Appearance.Themed) {
                colors.navigationBarTitle
            } else {
                textAttributes.foregroundColor ?: colors.navigationBarTitle
            },
        fit =
            TextFit(
                maxLines = if (isLong) HeaderFloats.LONG_CENTER_ITEM_TEXT_LINE_LIMIT.toInt() else 1,
                minimumScaleFactor = HeaderFloats.TEXT_MINIMUM_SCALE_FACTOR,
                overflow = TextOverflow.Ellipsis,
            ),
        font = textAttributes.font,
    )
}

@Composable
private fun PeripheralButton(
    buttonType: HeaderView.PeripheralButtonType,
    attributes: HeaderView.Attributes,
) {
    val colors = LocalPantherColors.current
    when (buttonType) {
        is HeaderView.PeripheralButtonType.Image -> {
            val imageAttributes = buttonType.attributes.image
            Components.Button(
                symbolName = imageAttributes.systemName,
                foregroundColor =
                    if (buttonType.attributes.isEnabled) {
                        imageAttributes.foregroundColor ?: colors.accent
                    } else {
                        colors.disabled
                    },
                onClick = { if (buttonType.attributes.isEnabled) buttonType.attributes.action() },
                modifier = imageAttributes.size?.let { Modifier.size(it) } ?: Modifier,
            )
        }

        is HeaderView.PeripheralButtonType.Text -> {
            val textAttributes = buttonType.attributes.text
            Components.Button(
                text = textAttributes.string,
                foregroundColor =
                    if (buttonType.attributes.isEnabled) {
                        textAttributes.foregroundColor ?: colors.accent
                    } else {
                        colors.disabled
                    },
                onClick = { if (buttonType.attributes.isEnabled) buttonType.attributes.action() },
                font = textAttributes.font,
            )
        }
    }
}
