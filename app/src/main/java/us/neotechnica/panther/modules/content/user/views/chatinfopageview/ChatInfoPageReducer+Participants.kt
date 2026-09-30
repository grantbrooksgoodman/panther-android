//
//  ChatInfoPageReducer+Participants.kt
//  Panther
//
//  Created by Grant Brooks Goodman.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import us.neotechnica.panther.designsystem.modules.alertkit.models.Action as AlertKitAction
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.ConfirmationAlert
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.session.entity.extensions.users
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.effect.Effect

/**
 * Presents the participant info alert – the user's language and region –
 * offering, in a group, to remove them from the conversation with a
 * confirmation. Mirrors iOS's `presentUserInfoAlert` and
 * `removeUserButtonTapped`.
 */
internal fun ChatInfoPageReducer.participantInfoEffect(
    state: ChatInfoPageReducer.State,
    userID: String,
): Effect<ChatInfoPageReducer.Action> =
    Effect.run { send ->
        val user =
            state.conversation?.users?.firstOrNull { it.id == userID }
                ?: SessionStore.users[userID]
                ?: return@run

        var didChooseRemove = false
        val actions =
            if (state.isGroup) {
                listOf(AlertKitAction("Remove from conversation", style = ActionStyle.DESTRUCTIVE) { didChooseRemove = true })
            } else {
                emptyList()
            }

        ActionSheetAlert(
            title = user.displayName,
            message = participantInfoMessage(user),
            actions = actions,
            cancelButtonTitle = LocalizedStringKey.Dismiss.localized(),
        ).present(
            // The title is the user's name and the message's labels are
            // pre-localized, but the group "Remove from conversation" action
            // is raw English, so translate the actions.
            translating = if (actions.isEmpty()) emptyList() else listOf(ActionSheetAlert.TranslationOptionKey.Actions()),
        )
        if (!didChooseRemove) return@run

        val didConfirm =
            ConfirmationAlert(
                title = user.displayName,
                message = "Are you sure you'd like to remove this person from the conversation?",
                cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                confirmButtonStyle = ActionStyle.DESTRUCTIVE_PREFERRED,
            ).present(
                translating =
                    listOf(
                        ConfirmationAlert.TranslationOptionKey.ConfirmButtonTitle,
                        ConfirmationAlert.TranslationOptionKey.Message,
                    ),
            )
        if (didConfirm) send(ChatInfoPageReducer.Action.RemoveParticipant(userID))
    }

private fun participantInfoMessage(user: User): String {
    val languageName = user.languageCode.uppercase()
    val regionName = RegionDetailService.localizedRegionName(user.phoneNumber.regionCode)
    return "${LocalizedStringKey.Language.localized()}: $languageName\n" +
        "${LocalizedStringKey.Region.localized()}: $regionName"
}
