//
//  ContactSelectorPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheetAlert
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.content.user.extensions.chatInfoPageLoadingStateUpdated
import us.neotechnica.panther.modules.content.user.extensions.currentConversationActivityChanged
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageReducer
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.schema.conversation.models.Conversation
import us.neotechnica.panther.networking.modules.schema.user.models.User
import us.neotechnica.panther.networking.modules.session.services.ActivitySessionService
import us.neotechnica.panther.networking.modules.session.services.ConversationSessionService
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents

/**
 * Handles the contact selector page's user interactions.
 *
 * The page is presented as a sheet either from the chat info page – to
 * add a participant to the conversation – or from the new chat page – to
 * choose a recipient.
 */
object ContactSelectorPageViewService {
    // MARK: - Properties

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Reducer Action Handlers

    /**
     * Dismisses the contact selector sheet without a selection.
     *
     * @param from The page from which the contact selector was
     *   presented.
     */
    @Suppress("UnusedParameter")
    fun cancelToolbarButtonTapped(from: ContactSelectorPageReducer.EntryPoint) {
        DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
    }

    /**
     * Returns the registered user with the given phone number.
     *
     * @param with The phone number to look up.
     *
     * @return The matching user.
     *
     * @throws Exception if no registered user matches, or if the lookup
     *   fails.
     */
    suspend fun findUser(with: PhoneNumber): User = UserService.getUser(with)

    /** Presents the invitation prompt. */
    fun inviteToolbarButtonTapped() {
        InviteService.presentInvitationPrompt()
    }

    /**
     * Presents an alert offering to invite the given phone number's
     * owner to the app. If the user accepts, the invitation prompt is
     * presented.
     *
     * @param phoneNumber The phone number for which no registered user
     *   was found.
     */
    suspend fun presentInvitationPrompt(phoneNumber: PhoneNumber) {
        val inviteAction = Action("Send Invite", style = ActionStyle.PREFERRED) { inviteToolbarButtonTapped() }
        Alert(
            title = phoneNumber.formattedString(),
            message =
                "Seems like there aren't any registered users with that phone number." +
                    "\n\nWould you like to invite them to sign up?",
            actions = listOf(inviteAction, Action(LocalizedStringKey.Cancel.localized(), style = ActionStyle.CANCEL) {}),
        ).present(
            translating = listOf(Alert.TranslationOptionKey.Actions(listOf(inviteAction)), Alert.TranslationOptionKey.Message),
        )
    }

    /**
     * Responds to the user selecting a contact on the contact selector
     * page.
     *
     * From the chat info page, an action sheet offers to add the selected
     * user to the current conversation; users already participating are
     * ignored. From the new chat page, the sheet is dismissed and the
     * selection is passed back through [onSelectForNewChat].
     *
     * @param selectedContactPair The contact the user selected.
     * @param from The page from which the contact selector was
     *   presented.
     * @param onSelectForNewChat Invoked with the selection's user id and
     *   display name when presented from the new chat page.
     */
    suspend fun selectedContactPairChanged(
        selectedContactPair: ContactMatch,
        from: ContactSelectorPageReducer.EntryPoint,
        onSelectForNewChat: (userID: String, displayName: String) -> Unit,
    ) {
        when (from) {
            ContactSelectorPageReducer.EntryPoint.CHAT_INFO_PAGE_VIEW -> {
                val userID = selectedContactPair.userID
                val conversation = ConversationSessionService.currentConversation ?: return
                if (conversation.participants.map { it.userID }.contains(userID)) return

                val addToConversationAction =
                    Action("Add to Conversation", style = ActionStyle.PREFERRED) {
                        serviceScope.launch { addToConversation(userID, conversation) }
                    }
                ActionSheetAlert(
                    message = selectedContactPair.fullName,
                    actions = listOf(addToConversationAction),
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                ).present(translating = listOf(ActionSheetAlert.TranslationOptionKey.Actions(emptyList())))
            }

            ContactSelectorPageReducer.EntryPoint.NEW_CHAT_PAGE_VIEW -> {
                DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
                onSelectForNewChat(selectedContactPair.userID, selectedContactPair.fullName)
            }
        }
    }

    // MARK: - Auxiliary

    private suspend fun addToConversation(
        userID: String,
        conversation: Conversation,
    ) {
        val navigation = DependencyValues.current.navigation
        val sharedEvents = DependencyValues.current.sharedEvents
        navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))
        sharedEvents.chatInfoPageLoadingStateUpdated.send(Unit)
        try {
            ActivitySessionService.addToConversation(userID, conversation)
            sharedEvents.currentConversationActivityChanged.send(Unit)
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
        }
    }
}
