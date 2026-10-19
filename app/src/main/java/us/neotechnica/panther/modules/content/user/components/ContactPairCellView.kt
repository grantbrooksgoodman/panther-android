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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.constants.ContactPairCellViewFloats
import us.neotechnica.panther.modules.content.user.dependencies.conversationCellViewService
import us.neotechnica.panther.modules.content.user.extensions.containsBlockedUser
import us.neotechnica.panther.modules.content.user.extensions.containsCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isSelected
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.services.ChatPageStateService
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.localization.models.localized

// MARK: - Constants Accessors

private typealias CellFloats = ContactPairCellViewFloats

/**
 * A row describing a contact pair in a contact list.
 *
 * Use [ContactPairCellView] to display a contact's name with a user
 * info badge and – when applicable – a blocked or "my account"
 * annotation. When given an action, the row becomes a button that is
 * disabled for contact pairs that contain a blocked user, contain the
 * current user, are already selected, or participate in the current
 * conversation.
 *
 * @param contactPair The contact pair the cell describes.
 * @param isInspectable A Boolean value that indicates whether the
 *   cell's name labels include a hidden inspection marker. Accepted
 *   for signature parity; Compose has no inspection marker.
 * @param modifier The modifier for this view.
 * @param contentPadding The padding between the cell's edges and its
 *   content. The padding responds to taps along with the content, so
 *   a list can inset its rows' content while its rows respond to taps
 *   from edge to edge.
 * @param action The action performed when the user taps the cell, or
 *   `null` to make the cell non-interactive.
 */
@Composable
fun ContactPairCellView(
    contactPair: ContactPair,
    @Suppress("UnusedParameter") isInspectable: Boolean = false,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    action: (() -> Unit)? = null,
) {
    val colors = LocalPantherColors.current
    val isSelectionEnabled = isSelectionEnabled(contactPair)
    val foregroundColor = if (isSelectionEnabled) colors.titleText else colors.disabled

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (action != null) Modifier.clickable(enabled = isSelectionEnabled, onClick = action) else Modifier)
                .padding(contentPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CellFloats.hStackSpacing),
        ) {
            if (contactPair.contact.firstName.isNotBlank()) {
                Components.Text(contactPair.contact.firstName, foregroundColor = foregroundColor)
            }

            Components.Text(contactPair.contact.lastName, foregroundColor = foregroundColor, font = Font.systemSemibold())

            if (contactPair.containsBlockedUser) {
                Components.Text("(${LocalizedStringKey.Blocked.localized()})", foregroundColor = foregroundColor)
            }

            if (contactPair.containsCurrentUser) {
                Components.Text(LocalizedStringKey.MyAccount.localized(), foregroundColor = foregroundColor)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        contactPair.users.firstOrNull()?.let { user ->
            UserInfoBadgeView(
                user = user,
                action = if (Build.isDeveloperModeEnabled) ({ DependencyValues.current.conversationCellViewService.presentUserInfoAlert(user) }) else null,
            )
        }
    }
}

// MARK: - Auxiliary

// Whether the current conversation is on screen: in its chat page, or in
// its chat info page, which is pushed in the chat page's place and so
// hides it.
private val isCurrentConversationPresented: Boolean
    get() {
        if (ChatPageStateService.isPresented) return true
        val topPath =
            DependencyValues.current.navigation.state.value.userContent.stack
                .lastOrNull()
        return topPath is UserContentNavigatorState.SeguePaths.ChatInfo &&
            topPath.conversationIDKey ==
            DependencyValues.current.clientSession.entity.conversation.currentConversation
                ?.id
                ?.key
    }

private fun isSelectionEnabled(contactPair: ContactPair): Boolean {
    val participantUserIDs =
        if (isCurrentConversationPresented) {
            DependencyValues.current.clientSession.entity.conversation.currentConversation
                ?.participants
                ?.map { it.userID }
        } else {
            null
        }
    val isParticipantInCurrentConversation =
        participantUserIDs != null && contactPair.userIDs.any { participantUserIDs.contains(it) }
    return !(
        contactPair.containsBlockedUser ||
            contactPair.containsCurrentUser ||
            contactPair.isSelected ||
            isParticipantInCurrentConversation
    )
}
