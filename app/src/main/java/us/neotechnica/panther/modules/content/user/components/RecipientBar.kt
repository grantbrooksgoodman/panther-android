//
//  RecipientBar.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.NewChatPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash

// MARK: - Constants Accessors

private typealias RecipientBarFloats = NewChatPageViewFloats

/** A mock recipient's chip is tinted red to signal it is unregistered. */
private val MOCK_CHIP_COLOR = Color(0xFFFF3B30)

/**
 * The recipient bar for the new chat page: a "To:" label, the selected
 * recipients as chips, an inline text field, and an add-contact button.
 *
 * @param selectedContactPairs The recipients currently selected.
 * @param query The text in the inline field.
 * @param highlightedViewID The encoded hash of the recipient highlighted
 *   for deletion, or `null`.
 * @param onQueryChange Invoked as the field text changes.
 * @param onSubmit Invoked when the field is submitted.
 * @param onBackspace Invoked on a backspace in the empty field.
 * @param onChipTap Invoked with a recipient's view identifier when its
 *   chip is tapped.
 * @param onRemove Invoked with a recipient's view identifier to remove it.
 * @param onAdd Invoked when the add-contact button is tapped.
 */
@Composable
@Suppress("LongParameterList")
fun RecipientBar(
    selectedContactPairs: List<ContactPair>,
    query: String,
    highlightedViewID: String?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit,
    onChipTap: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalPantherColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RecipientBarFloats.recipientBarHorizontalPadding,
                    vertical = RecipientBarFloats.recipientBarVerticalPadding,
                ).clip(RoundedCornerShape(RecipientBarFloats.recipientBarCornerRadius))
                .background(colors.background)
                .padding(
                    start = RecipientBarFloats.recipientBarStartPadding,
                    end = RecipientBarFloats.recipientBarEndPadding,
                    top = RecipientBarFloats.recipientBarTopPadding,
                    bottom = RecipientBarFloats.recipientBarBottomPadding,
                ),
    ) {
        Components.Text(
            LocalizedStringKey.To.localized(),
            color = colors.subtitleText,
            modifier = Modifier.padding(end = RecipientBarFloats.toLabelEndPadding),
        )

        RecipientBarContent(
            selectedContactPairs = selectedContactPairs,
            query = query,
            highlightedViewID = highlightedViewID,
            onQueryChange = onQueryChange,
            onSubmit = onSubmit,
            onBackspace = onBackspace,
            onChipTap = onChipTap,
            onRemove = onRemove,
            modifier = Modifier.weight(1f),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(start = RecipientBarFloats.addButtonStartPadding)
                    .size(RecipientBarFloats.addButtonSize)
                    .clip(CircleShape)
                    .background(colors.groupedContentBackground)
                    .clickable(onClick = onAdd),
        ) {
            Components.Symbol("plus", color = colors.accent, modifier = Modifier.size(RecipientBarFloats.addButtonGlyphSize))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun RecipientBarContent(
    selectedContactPairs: List<ContactPair>,
    query: String,
    highlightedViewID: String?,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit,
    onChipTap: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(RecipientBarFloats.chipSpacing),
        verticalArrangement = Arrangement.spacedBy(RecipientBarFloats.chipSpacing),
        modifier = modifier,
    ) {
        selectedContactPairs.forEach { contactPair ->
            val viewID = contactPair.contact.encodedHash
            RecipientChip(
                contactPair = contactPair,
                isHighlighted = viewID == highlightedViewID,
                onTap = { onChipTap(viewID) },
                onRemove = { onRemove(viewID) },
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = Font.system.textStyle.copy(color = colors.titleText),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier =
                Modifier
                    .defaultMinSize(minWidth = RecipientBarFloats.fieldMinWidth)
                    .padding(vertical = RecipientBarFloats.fieldVerticalPadding)
                    .onPreviewKeyEvent { event ->
                        val isBackspaceOnEmpty =
                            event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                query.isEmpty() &&
                                selectedContactPairs.isNotEmpty()
                        if (isBackspaceOnEmpty) {
                            onBackspace()
                            true
                        } else {
                            false
                        }
                    },
        )
    }
}

@Composable
private fun RecipientChip(
    contactPair: ContactPair,
    isHighlighted: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val chipColor = if (contactPair.isMock) MOCK_CHIP_COLOR else colors.accent
    val backgroundColor = if (isHighlighted) chipColor else chipColor.copy(alpha = RecipientBarFloats.CHIP_BACKGROUND_ALPHA)
    val contentColor = if (isHighlighted) Color.White else chipColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onTap)
                .padding(
                    start = RecipientBarFloats.recipientChipStartPadding,
                    end = RecipientBarFloats.recipientChipEndPadding,
                    top = RecipientBarFloats.recipientChipVerticalPadding,
                    bottom = RecipientBarFloats.recipientChipVerticalPadding,
                ),
    ) {
        Components.Text(contactPair.contact.fullName, color = contentColor, font = Font.systemMedium(FontScale.Small))
        Components.Symbol(
            "xmark",
            color = contentColor,
            modifier =
                Modifier
                    .padding(start = RecipientBarFloats.chipRemoveIconStartPadding)
                    .size(RecipientBarFloats.chipRemoveIconSize)
                    .clickable(onClick = onRemove),
        )
    }
}
