//
//  RecipientBar.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.neotechnica.panther.R
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceColors
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarLayoutService
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.time.Duration.Companion.milliseconds

// MARK: - Constants Accessors

private typealias LayoutColors = ChatPageViewServiceColors.RecipientBarService.Layout
private typealias LayoutFloats = ChatPageViewServiceFloats.RecipientBarService.Layout
private typealias LayoutStrings = ChatPageViewServiceStrings.RecipientBarService.Layout
private typealias SelectionColors = ChatPageViewServiceColors.RecipientBarService.ContactSelectionUI
private typealias SelectionFloats = ChatPageViewServiceFloats.RecipientBarService.ContactSelectionUI
private typealias SelectionStrings = ChatPageViewServiceStrings.RecipientBarService.ContactSelectionUI
private typealias TextFieldDelegateFloats = ChatPageViewServiceFloats.RecipientBarService.UITextFieldDelegate

/**
 * The bar for choosing recipients when composing a new conversation.
 *
 * [RecipientBar] hosts a text field for entering a recipient's name
 * or phone number, the selected recipients as contact views, and the
 * select-contact button. The bar delegates its behavior to
 * [RecipientBarService]; this composable renders the service's state.
 *
 * @param modifier The modifier for this view.
 */
@Composable
fun RecipientBar(modifier: Modifier = Modifier) {
    val colors = LocalPantherColors.current
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val service = RecipientBarService

    val selectedContactPairs by service.contactSelectionUI.selectedContactPairs.collectAsState()
    val highlightedViewID by service.contactSelectionUI.highlightedViewID.collectAsState()
    val isLabelRepresentationOn by service.contactSelectionUI.isLabelRepresentationOn.collectAsState()
    val isSelectContactButtonEnabled by service.layout.isSelectContactButtonEnabled.collectAsState()
    val isUserInteractionEnabled by service.layout.isUserInteractionEnabled.collectAsState()
    val textFieldText by service.layout.textFieldText.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Subscribe before the first layout pass so its focus request is
    // not dropped, and let the field be placed before focusing it.
    LaunchedEffect(Unit) {
        service.layout.firstResponderRequests.collect { responder ->
            if (responder == RecipientBarLayoutService.FirstResponder.TEXT_FIELD) {
                withFrameNanos {}
                runCatching { focusRequester.requestFocus() }
            }
        }
    }
    LaunchedEffect(Unit) { service.onLayoutSubviews() }

    val borderColor =
        if (!ThemeService.isAppDefaultThemeApplied || isDarkModeActive) LayoutColors.darkBorder else LayoutColors.lightBorder
    val backgroundColor =
        if (isDarkModeActive) {
            if (ThemeService.isAppDefaultThemeApplied) colors.groupedContentBackground else colors.background
        } else {
            LayoutColors.lightBackground.copy(alpha = LayoutFloats.LIGHT_BACKGROUND_COLOR_ALPHA_COMPONENT)
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .testTag(LayoutStrings.RECIPIENT_BAR_SEMANTIC_TAG)
                .then(
                    if (isLabelRepresentationOn) {
                        Modifier.clickable { service.layout.textFieldBecomeFirstResponder() }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Border(borderColor)

        // The bar grows a row at a time to fit its contact views, which keep
        // the inset from its edges that centering the first row in the bar's
        // initial height gives them. The "To:" label and the select contact
        // button stay centered on the first row, as on iOS.
        Row(modifier = Modifier.fillMaxWidth()) {
            Components.Text(
                LocalizedStringKey.To.localized(),
                foregroundColor = LayoutColors.toLabelText,
                font = Font.system(FontScale.Custom(LayoutFloats.TO_LABEL_FONT_SIZE)),
                modifier =
                    Modifier
                        .padding(start = LayoutFloats.toLabelXOrigin)
                        .height(LayoutFloats.frameHeight)
                        .wrapContentHeight()
                        .testTag(LayoutStrings.TO_LABEL_SEMANTIC_TAG),
            )

            ContactViews(
                selectedContactPairs = selectedContactPairs,
                highlightedViewID = highlightedViewID,
                isLabelRepresentationOn = isLabelRepresentationOn,
                isUserInteractionEnabled = isUserInteractionEnabled,
                textFieldText = textFieldText,
                focusRequester = focusRequester,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = LayoutFloats.textFieldXOriginIncrement)
                        .padding(vertical = (LayoutFloats.frameHeight - SelectionFloats.contactViewFrameHeight) / 2),
            )

            Icon(
                painter = painterResource(R.drawable.plus_circle),
                contentDescription = null,
                tint = if (isSelectContactButtonEnabled) colors.navigationBarButton else colors.disabled,
                modifier =
                    Modifier
                        .padding(start = LayoutFloats.selectContactButtonMinXDecrement)
                        .padding(end = LayoutFloats.selectContactButtonXOriginDecrement)
                        .height(LayoutFloats.frameHeight)
                        .wrapContentHeight()
                        .size(width = LayoutFloats.selectContactButtonFrameWidth, height = LayoutFloats.selectContactButtonFrameHeight)
                        .testTag(LayoutStrings.SELECT_CONTACT_BUTTON_SEMANTIC_TAG)
                        .clickable(enabled = isSelectContactButtonEnabled && isUserInteractionEnabled) {
                            scope.launch { service.actionHandler.selectContactButtonTapped() }
                        },
            )
        }

        Border(borderColor)
    }
}

// MARK: - Contact Views

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun ContactViews(
    selectedContactPairs: List<ContactPair>,
    highlightedViewID: String?,
    isLabelRepresentationOn: Boolean,
    isUserInteractionEnabled: Boolean,
    textFieldText: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val service = RecipientBarService
    val scope = rememberCoroutineScope()
    val maximumContactViewWidth = (LocalConfiguration.current.screenWidthDp / SelectionFloats.CONTACT_VIEW_MAXIMUM_WIDTH_DIVISOR).dp

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SelectionFloats.adjacentViewSpacing),
        verticalArrangement = Arrangement.spacedBy(SelectionFloats.adjacentViewSpacing),
        modifier = modifier,
    ) {
        selectedContactPairs.forEachIndexed { index, contactPair ->
            val viewID = contactPair.contact.encodedHash
            val isLast = index == selectedContactPairs.lastIndex
            ContactView(
                contactPair = contactPair,
                isHighlighted = viewID == highlightedViewID,
                isLabelRepresentationOn = isLabelRepresentationOn,
                showsTrailingComma = isLabelRepresentationOn && !isLast,
                maximumWidth = maximumContactViewWidth,
                onTap =
                    if (isLast && !isLabelRepresentationOn && isUserInteractionEnabled) {
                        { service.contactSelectionUI.toggleIsHighlighted(viewID = viewID) }
                    } else {
                        null
                    },
            )
        }

        // The delegate reacts to editing beginning and ending, not to the
        // field's initial unfocused state.
        var wasFocused by remember { mutableStateOf(false) }

        fun focusChanged(isFocused: Boolean) {
            if (isFocused || wasFocused) textFieldFocusChanged(isFocused)
            wasFocused = isFocused
        }

        BasicTextField(
            value = textFieldText,
            onValueChange = { service.actionHandler.textFieldChanged(it) },
            enabled = isUserInteractionEnabled,
            singleLine = true,
            textStyle = Font.system.textStyle.copy(color = colors.titleText),
            cursorBrush = SolidColor(colors.navigationBarButton),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { scope.launch { service.actionHandler.textFieldShouldReturn(textFieldText) } }),
            // The field is as tall as a contact view; its text is centered
            // vertically within that height.
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) { innerTextField() }
            },
            modifier =
                Modifier
                    .defaultMinSize(minWidth = TEXT_FIELD_MIN_WIDTH)
                    .height(SelectionFloats.contactViewFrameHeight)
                    .focusRequester(focusRequester)
                    .testTag(LayoutStrings.TEXT_FIELD_SEMANTIC_TAG)
                    .onFocusChanged { focusChanged(it.isFocused) }
                    .onPreviewKeyEvent { event ->
                        val isBackspaceOnEmpty =
                            event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                textFieldText.isEmpty() &&
                                selectedContactPairs.isNotEmpty()
                        if (isBackspaceOnEmpty) service.actionHandler.onSuperflousBackspace()
                        isBackspaceOnEmpty
                    },
        )
    }
}

// Mirrors the text field delegate: beginning editing leaves the label
// representation; ending it restores the representation after a short
// delay unless the field regained focus.
private fun textFieldFocusChanged(isFocused: Boolean) {
    val service = RecipientBarService
    service.layout.setIsTextFieldFirstResponder(isFocused)

    if (isFocused) {
        service.contactSelectionUI.toggleLabelRepresentation(on = false)
        return
    }

    Task.delayed(by = TextFieldDelegateFloats.TOGGLE_LABEL_REPRESENTATION_DELAY_MILLISECONDS.milliseconds) {
        if (service.layout.isTextFieldFirstResponder.value) return@delayed
        service.contactSelectionUI.toggleLabelRepresentation(on = true)
    }
}

// MARK: - Contact View

@Composable
@Suppress("LongParameterList")
private fun ContactView(
    contactPair: ContactPair,
    isHighlighted: Boolean,
    isLabelRepresentationOn: Boolean,
    showsTrailingComma: Boolean,
    maximumWidth: androidx.compose.ui.unit.Dp,
    onTap: (() -> Unit)?,
) {
    val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val labelColor = if (contactPair.isMock) SelectionColors.contactViewRedText else SelectionColors.accent
    val selectionColor = if (isDarkModeActive) SelectionColors.contactViewDarkSelection else SelectionColors.contactViewLightSelection

    val backgroundColor =
        when {
            isLabelRepresentationOn -> SelectionColors.labelRepresentationColor
            isHighlighted -> labelColor
            else -> selectionColor
        }
    val textColor = if (isHighlighted && !isLabelRepresentationOn) SelectionColors.contactViewHighlightedText else labelColor
    val text = if (showsTrailingComma) "${contactPair.contact.fullName}," else contactPair.contact.fullName

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .height(SelectionFloats.contactViewFrameHeight)
                .clip(RoundedCornerShape(SelectionFloats.contactViewCornerRadius))
                .background(backgroundColor)
                .border(CONTACT_VIEW_BORDER_WIDTH, backgroundColor, RoundedCornerShape(SelectionFloats.contactViewCornerRadius))
                .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
                .padding(horizontal = SelectionFloats.contactViewWidthIncrement / 2)
                .testTag(SelectionStrings.CONTACT_VIEW_SEMANTIC_TAG),
    ) {
        Components.Text(
            text,
            foregroundColor = textColor,
            font = Font.system(FontScale.Custom(SelectionFloats.CONTACT_LABEL_SYSTEM_FONT_SIZE)),
            modifier = Modifier.widthIn(max = maximumWidth).testTag(SelectionStrings.CONTACT_LABEL_SEMANTIC_TAG),
        )
    }
}

// MARK: - Border

@Composable
private fun Border(color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(LayoutFloats.borderHeight).background(color))
}

// MARK: - Constants

private val CONTACT_VIEW_BORDER_WIDTH = 1.dp
private val TEXT_FIELD_MIN_WIDTH = 120.dp
