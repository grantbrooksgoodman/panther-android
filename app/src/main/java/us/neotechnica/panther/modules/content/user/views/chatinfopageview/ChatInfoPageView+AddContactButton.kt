//
//  ChatInfoPageView+AddContactButton.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.SFSymbol
import us.neotechnica.panther.designsystem.modules.foundation.constants.StackFloats
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Constants Accessors

private typealias ButtonColors = ChatInfoPageViewColors
private typealias ButtonFloats = ChatInfoPageViewFloats
private typealias ButtonStrings = ChatInfoPageViewConstants

// MARK: - Add Contact Button

// The add-contact row: a plus symbol in a circle followed by a label.
// Tapping it presents the contact selector.
@Composable
internal fun AddContactButton(
    state: ChatInfoPageReducer.State,
    viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>,
) {
    val colors = LocalPantherColors.current
    val isEnabled = state.isAddContactButtonEnabled
    val circleColor =
        if (ThemeService.isDarkModeActive(isSystemInDarkTheme())) {
            ButtonColors.addContactButtonCircleDarkForeground
        } else {
            ButtonColors.addContactButtonCircleLightForeground
        }
    val symbolColor = if (isEnabled) ButtonColors.addContactButtonSymbolForeground else colors.disabled
    val labelColor = if (isEnabled) ButtonColors.addContactButtonLabelForeground else colors.disabled

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StackFloats.defaultSpacing),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) { viewModel.send(ChatInfoPageReducer.Action.AddContactButtonTapped) }
                .padding(horizontal = ButtonFloats.cardHorizontalPadding, vertical = ButtonFloats.rowVerticalPadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(end = ButtonFloats.addContactButtonCircleTrailingPadding)
                    .size(
                        width = ButtonFloats.addContactButtonCircleFrameMaxWidth,
                        height = ButtonFloats.addContactButtonCircleFrameMaxHeight,
                    ).clip(CircleShape)
                    .background(circleColor),
        ) {
            Components.Symbol(
                ButtonStrings.ADD_CONTACT_BUTTON_IMAGE_SYSTEM_NAME,
                foregroundColor = symbolColor,
                usesIntrinsicSize = false,
                modifier =
                    if (SFSymbol.resolvesToSFSymbol(ButtonStrings.ADD_CONTACT_BUTTON_IMAGE_SYSTEM_NAME)) {
                        Modifier.size(
                            width = ButtonFloats.addContactButtonSymbolImageWidth,
                            height = ButtonFloats.addContactButtonSymbolImageHeight,
                        )
                    } else {
                        Modifier.size(
                            width = ButtonFloats.addContactButtonImageWidth,
                            height = ButtonFloats.addContactButtonImageHeight,
                        )
                    },
            )
        }

        Components.Text(
            state.strings.value(ChatInfoPageViewStrings.addContactButtonText),
            foregroundColor = labelColor,
        )
    }
}
