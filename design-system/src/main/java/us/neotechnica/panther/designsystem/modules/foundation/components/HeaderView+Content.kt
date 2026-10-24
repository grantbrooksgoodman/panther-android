//
//  HeaderView+Content.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.TextFit
import us.neotechnica.panther.designsystem.modules.foundation.constants.HeaderViewColors
import us.neotechnica.panther.designsystem.modules.foundation.constants.HeaderViewFloats
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors

// MARK: - Content

@Composable
internal fun ContentView(
    leftItem: HeaderView.PeripheralButtonType?,
    centerItem: HeaderView.CenterItemType?,
    rightItem: HeaderView.PeripheralButtonType?,
    attributes: HeaderView.Attributes,
) {
    val minHeight =
        when (val sizeClass = attributes.sizeClass) {
            is HeaderView.SizeClass.Custom -> sizeClass.minHeight
            HeaderView.SizeClass.FullScreenCover -> HeaderViewFloats.FULL_SCREEN_COVER_SIZE_CLASS_FRAME_MIN_HEIGHT
            HeaderView.SizeClass.Sheet -> HeaderViewFloats.SHEET_SIZE_CLASS_FRAME_MIN_HEIGHT
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(HeaderViewFloats.ITEM_SPACING.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.dp)
                .padding(horizontal = HeaderViewFloats.HORIZONTAL_PADDING.dp),
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
internal fun CenterImage(
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
                Modifier.sizeIn(maxHeight = HeaderViewFloats.IMAGE_MAX_HEIGHT.dp)
            },
    )
}

@Composable
internal fun CenterText(
    textAttributes: HeaderView.TextAttributes,
    attributes: HeaderView.Attributes,
) {
    val colors = LocalPantherColors.current
    val isLong =
        textAttributes.string.length >= HeaderViewFloats.LONG_CENTER_ITEM_TEXT_CHARACTER_COUNT_THRESHOLD.toInt()
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
                maxLines = if (isLong) HeaderViewFloats.LONG_CENTER_ITEM_TEXT_LINE_LIMIT.toInt() else 1,
                minimumScaleFactor = HeaderViewFloats.TEXT_MINIMUM_SCALE_FACTOR,
                overflow = TextOverflow.Ellipsis,
            ),
        font = textAttributes.font,
    )
}

@Composable
internal fun PeripheralButton(
    buttonType: HeaderView.PeripheralButtonType,
    attributes: HeaderView.Attributes,
) {
    val colors = LocalPantherColors.current
    when (buttonType) {
        is HeaderView.PeripheralButtonType.Image -> {
            val imageAttributes = buttonType.attributes.image
            val tint =
                if (buttonType.attributes.isEnabled) {
                    imageAttributes.foregroundColor ?: colors.accent
                } else {
                    colors.disabled
                }

            val onClick = { if (buttonType.attributes.isEnabled) buttonType.attributes.action() }
            val modifier = imageAttributes.size?.let { Modifier.size(it) } ?: Modifier
            val drawableResourceID = imageAttributes.drawableResourceID
            if (drawableResourceID == null) {
                Components.Button(
                    symbolName = imageAttributes.systemName,
                    foregroundColor = tint,
                    onClick = onClick,
                    modifier = modifier,
                )
            } else {
                Icon(
                    contentDescription = null,
                    modifier = modifier.clickable(onClick = onClick),
                    painter = painterResource(drawableResourceID),
                    tint = tint,
                )
            }
        }

        is HeaderView.PeripheralButtonType.Text -> {
            val textAttributes = buttonType.attributes.text

            // A title too wide for its side of the header shrinks on one
            // line rather than wrapping.
            Components.FittedText(
                textAttributes.string,
                foregroundColor =
                    if (buttonType.attributes.isEnabled) {
                        textAttributes.foregroundColor ?: colors.accent
                    } else {
                        colors.disabled
                    },
                fit =
                    TextFit(
                        maxLines = 1,
                        minimumScaleFactor = HeaderViewFloats.TEXT_MINIMUM_SCALE_FACTOR,
                        overflow = TextOverflow.Ellipsis,
                    ),
                font = textAttributes.font,
                modifier = Modifier.clickable { if (buttonType.attributes.isEnabled) buttonType.attributes.action() },
            )
        }
    }
}
