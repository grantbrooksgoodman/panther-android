//
//  ContactPairCellView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.ContactPairCellViewFloats
import us.neotechnica.panther.modules.content.user.extensions.containsBlockedUser
import us.neotechnica.panther.modules.content.user.extensions.containsCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isSelected
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.services.ConversationCellViewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * A row describing a contact pair in a contact list.
 *
 * Use [ContactPairCellView] to display a contact's name with a user
 * info badge and, when applicable, a blocked or "my account"
 * annotation. When given an action, the row becomes a button that is
 * disabled for contact pairs that contain a blocked user, contain the
 * current user, are already selected, or participate in the current
 * conversation.
 *
 * @param contactPair The contact pair the cell describes.
 * @param isInspectable Reserved for a future inspection affordance;
 *   currently has no effect.
 * @param action The action performed when the user taps the cell, or
 *   `null` to make the cell non-interactive.
 */
@Composable
fun ContactPairCellView(
    contactPair: ContactPair,
    @Suppress("UnusedParameter") isInspectable: Boolean = false,
    action: (() -> Unit)? = null,
) {
    val colors = LocalPantherColors.current
    val isSelectionEnabled = isSelectionEnabled(contactPair)
    val foregroundColor = if (isSelectionEnabled) colors.titleText else colors.disabled

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (action != null) Modifier.clickable(enabled = isSelectionEnabled, onClick = action) else Modifier)
                .padding(
                    horizontal = ContactPairCellViewFloats.horizontalPadding,
                    vertical = ContactPairCellViewFloats.verticalPadding,
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ContactPairCellViewFloats.hStackSpacing),
        ) {
            if (contactPair.contact.firstName.isNotBlank()) {
                Components.Text(contactPair.contact.firstName, color = foregroundColor)
            }

            Components.Text(contactPair.contact.lastName, color = foregroundColor, font = Font.systemSemibold())

            if (contactPair.containsBlockedUser) {
                Components.Text("(${LocalizedStringKey.Blocked.localized()})", color = foregroundColor)
            }

            if (contactPair.containsCurrentUser) {
                Components.Text(LocalizedStringKey.MyAccount.localized(), color = foregroundColor)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        contactPair.users.firstOrNull()?.let { user ->
            UserInfoBadgeView(
                user = user,
                action = if (Build.isDeveloperModeEnabled) ({ ConversationCellViewService.presentUserInfoAlert(user) }) else null,
            )
        }
    }
}

// MARK: - Auxiliary

private fun isSelectionEnabled(contactPair: ContactPair): Boolean {
    val participantUserIDs = ConversationSessionService.currentConversation?.participants?.map { it.userID }
    val isParticipantInCurrentConversation =
        participantUserIDs != null && contactPair.userIDs.any { participantUserIDs.contains(it) }
    return !(
        contactPair.containsBlockedUser ||
            contactPair.containsCurrentUser ||
            contactPair.isSelected ||
            isParticipantInCurrentConversation
    )
}
