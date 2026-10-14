//
//  ContactSelectorPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.chatInfoPageLoadingStateUpdated
import us.neotechnica.panther.bundle.currentConversationActivityChanged
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.designsystem.modules.alertkit.models.Alert
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.userIDs
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.services.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.modules.networking.user.services.UserService
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ActivitySessionService
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles the contact selector page's user interactions.
 *
 * The page is presented as a sheet either from the chat info page – to
 * add a participant to the conversation – or from the new chat page – to
 * choose a recipient.
 */
object ContactSelectorPageViewService {
    // MARK: - Dependencies

    private val activitySession: ActivitySessionService by Dependency { it.clientSession.entity.activity }
    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val inviteService: InviteService by Dependency { it.commonServices.invite }

    // MARK: - Properties

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Reducer Action Handlers

    /**
     * Dismisses the contact selector sheet without a selection.
     *
     * When entered from the new chat page, the contact selection UI's
     * label representation is updated to match the recipient bar's
     * focus.
     *
     * @param from The page from which the contact selector was
     *   presented.
     */
    fun cancelToolbarButtonTapped(from: ContactSelectorPageView.EntryPoint) {
        DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))

        if (from != ContactSelectorPageView.EntryPoint.NEW_CHAT_PAGE_VIEW) return
        Task.delayed(by = INPUT_BAR_APPEARANCE_DELAY) {
            Task.delayed(by = LABEL_REPRESENTATION_DELAY) {
                val recipientBarIsFirstResponder = RecipientBarService.layout.isTextFieldFirstResponder.value
                RecipientBarService.contactSelectionUI.toggleLabelRepresentation(on = !recipientBarIsFirstResponder)
            }
        }
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

    /** Presents the invitation prompt, surfacing any error as a toast. */
    fun inviteToolbarButtonTapped() {
        serviceScope.launch {
            runCatching { inviteService.presentInvitationPrompt() }
                .onFailure { Logger.log(it as? Exception ?: Exception.from(it, ExceptionMetadata(this)), with = AlertType.toast) }
        }
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
     * Responds to the user selecting a contact pair on the contact
     * selector page.
     *
     * When entered from the chat info page, an action sheet offers to
     * add the selected user to the current conversation – users already
     * participating are ignored. When entered from the new chat page,
     * the sheet is dismissed and the selection is applied to the
     * recipient bar.
     *
     * @param selectedContactPair The contact pair the user selected.
     * @param from The page from which the contact selector was
     *   presented.
     */
    suspend fun selectedContactPairChanged(
        selectedContactPair: ContactPair,
        from: ContactSelectorPageView.EntryPoint,
    ) {
        when (from) {
            ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW -> {
                val user = selectedContactPair.users.firstOrNull() ?: return
                val userID = selectedContactPair.userIDs.firstOrNull() ?: return
                val conversation = conversationSession.currentConversation ?: return
                if (conversation.participants.map { it.userID }.contains(userID)) return

                val addToConversationAction =
                    Action("Add to Conversation", style = ActionStyle.PREFERRED) {
                        serviceScope.launch { addToConversation(userID, conversation) }
                    }
                ActionSheet(
                    message = user.displayName,
                    actions = listOf(addToConversationAction),
                    cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
                ).present(translating = listOf(ActionSheet.TranslationOptionKey.Actions(emptyList())))
            }

            ContactSelectorPageView.EntryPoint.NEW_CHAT_PAGE_VIEW -> {
                DependencyValues.current.navigation.navigate(Route.Chat(ChatRoute.Sheet(null)))

                // Dismissing the sheet closes the page's view model, so the
                // deferred selection runs on a detached task.
                Task.delayed(by = INPUT_BAR_APPEARANCE_DELAY) {
                    RecipientBarService.contactSelectionUI.selectContactPair(selectedContactPair, performInputBarFix = true)

                    delay(LABEL_REPRESENTATION_DELAY)
                    val recipientBarIsFirstResponder = RecipientBarService.layout.isTextFieldFirstResponder.value
                    RecipientBarService.contactSelectionUI.toggleLabelRepresentation(on = !recipientBarIsFirstResponder)
                }
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
            activitySession.addToConversation(userID, conversation)
            sharedEvents.currentConversationActivityChanged.send(Unit)
        } catch (exception: Exception) {
            Logger.log(exception, with = AlertType.toast)
        }
    }

    // MARK: - Constants

    private val INPUT_BAR_APPEARANCE_DELAY = 100.milliseconds
    private val LABEL_REPRESENTATION_DELAY = 200.milliseconds
}
