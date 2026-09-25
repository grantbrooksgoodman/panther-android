//
//  ChatInfoAddContact.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewColors
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewConstants
import us.neotechnica.panther.modules.content.user.constants.ChatInfoPageViewFloats
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageReducer
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * The chat info page's add-contact row: a plus glyph in a circle followed
 * by a label. Tapping it opens the contact selector.
 *
 * @param text The row's label.
 * @param isEnabled Whether the row responds to taps; when `false`, its
 *   symbol and label are dimmed and it ignores taps.
 * @param onClick Invoked when the row is tapped.
 */
@Composable
fun AddContactButton(
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val isDark = ThemeService.isDarkModeActive(isSystemInDarkTheme())
    val circleColor =
        if (isDark) {
            ChatInfoPageViewColors.addContactButtonCircleDarkForeground
        } else {
            ChatInfoPageViewColors.addContactButtonCircleLightForeground
        }
    val contentColor = if (isEnabled) colors.accent else colors.disabled
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (isEnabled) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = ChatInfoPageViewFloats.cardHorizontalPadding, vertical = ChatInfoPageViewFloats.rowVerticalPadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(end = ChatInfoPageViewFloats.addContactButtonCircleTrailingPadding)
                    .size(
                        width = ChatInfoPageViewFloats.addContactButtonCircleFrameMaxWidth,
                        height = ChatInfoPageViewFloats.addContactButtonCircleFrameMaxHeight,
                    ).clip(CircleShape)
                    .background(circleColor),
        ) {
            Components.Symbol(
                ChatInfoPageViewConstants.ADD_CONTACT_BUTTON_IMAGE_SYSTEM_NAME,
                color = contentColor,
                modifier =
                    Modifier.size(
                        width = ChatInfoPageViewFloats.addContactButtonImageWidth,
                        height = ChatInfoPageViewFloats.addContactButtonImageHeight,
                    ),
            )
        }
        Components.Text(text, color = contentColor, modifier = Modifier.padding(start = ChatInfoPageViewFloats.rowTextStartPadding))
    }
}

/**
 * Presents the contact selector over the chat info page while the chat
 * flow's sheet state selects it.
 */
@Composable
fun ChatInfoContactSelectorHost() {
    val navigation = remember { DependencyValues.current.navigation }
    val navState by navigation.state.collectAsState()
    if (navState.chat.sheet == ChatNavigatorState.SheetPath.ContactSelector) {
        ContactSelectorPageView(entryPoint = ContactSelectorPageReducer.EntryPoint.CHAT_INFO_PAGE_VIEW)
    }
}
