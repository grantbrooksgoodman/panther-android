//
//  RecipientBarActionHandlerService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources

import us.neotechnica.panther.bundle.contactAccessDenied
import us.neotechnica.panther.bundle.mismatchedHashAndCallingCode
import us.neotechnica.panther.bundle.navigators.ChatNavigatorState
import us.neotechnica.panther.bundle.navigators.ChatRoute
import us.neotechnica.panther.designsystem.modules.foundation.hud.HUD
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.PermissionService
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.hasContactsBesidesCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.isMock
import us.neotechnica.panther.modules.content.user.extensions.mock
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.networking.sources.user.services.UserService
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// MARK: - Constants Accessors

private typealias ActionHandlerFloats = ChatPageViewServiceFloats.RecipientBarService.ActionHandler

/**
 * The service that handles the recipient bar's user actions.
 *
 * [RecipientBarActionHandlerService] responds to the recipient bar's
 * interactions: tapping the select-contact button, editing and
 * submitting the text field, and deleting a recipient with a backspace
 * on an empty field.
 *
 * @param dependencies The collaborators the handler reaches. The
 *   default reaches the shared services.
 */
class RecipientBarActionHandlerService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Dependencies

    // MARK: - Types

    /**
     * The collaborators the action handler reaches, each replaceable
     * for testing.
     */
    @Suppress("LongParameterList")
    class Dependencies(
        /** Returns the contacts permission status. */
        val contactPermissionStatus: () -> PermissionService.PermissionStatus = {
            DependencyValues.current.commonServices.permission.contactPermissionStatus
        },
        /** Returns whether the contact pair archive holds contacts besides the current user. */
        val hasContactsBesidesCurrentUser: () -> Boolean = {
            DependencyValues.current.commonServices.contact.hasContactsBesidesCurrentUser
        },
        /** Presents the contacts call to action. */
        val presentContactCTA: suspend () -> Unit = {
            DependencyValues.current.commonServices.permission
                .presentCTA(PermissionService.PermissionType.CONTACTS)
        },
        /** Presents the invitation suggestion prompt. */
        val presentInvitationSuggestionPrompt: suspend () -> Unit = {
            DependencyValues.current.commonServices.invite
                .presentInvitationSuggestionPrompt()
        },
        /** Requests the contacts permission. */
        val requestContactPermission: suspend () -> PermissionService.PermissionStatus = {
            DependencyValues.current.commonServices.permission
                .requestPermission(PermissionService.PermissionType.CONTACTS)
        },
        /** Synchronizes the contact pair archive. */
        val syncContactPairArchive: suspend () -> Unit = {
            DependencyValues.current.commonServices.contact
                .syncContactPairArchive()
        },
        /** Returns the registered user with the given phone number. */
        val getUser: suspend (PhoneNumber) -> User = { UserService.getUser(it) },
        /** Navigates to the given route. */
        val navigate: (Route) -> Unit = { DependencyValues.current.navigation.navigate(it) },
        /** Hides the progress HUD after the given delay. */
        val hideHUD: () -> Unit = { HUD.hide(after = HUD_HIDE_DELAY) },
        /** Shows the modal progress HUD. */
        val showProgressHUD: () -> Unit = { HUD.showProgress(isModal = true) },
        /** Returns the recipient bar service. */
        val recipientBar: () -> RecipientBarService = { RecipientBarService },
    )

    // MARK: - On Superfluous Backspace

    /**
     * Handles a backspace on the empty text field by highlighting, then
     * removing, the last recipient.
     *
     * The first backspace highlights the trailing-most recipient; a
     * second backspace removes it and reconfigures the message list.
     */
    fun onSuperflousBackspace() {
        val service = dependencies.recipientBar()
        val lastContactPair =
            service.contactSelectionUI.selectedContactPairs.value
                .lastOrNull() ?: return
        val lastViewID = lastContactPair.contact.encodedHash

        if (!service.contactSelectionUI.isHighlighted(viewID = lastViewID)) {
            service.contactSelectionUI.toggleIsHighlighted(viewID = lastViewID)
            return
        }

        service.contactSelectionUI.deselectContactPair(withViewID = lastViewID)
        service.config.reconfigureCollectionView()
    }

    // MARK: - Selector Action Handlers

    /**
     * Handles a tap on the select-contact button by presenting the
     * contact selector.
     *
     * If the contact permission has not been granted, this method
     * requests it and presents a call to action when it is denied. If
     * no contacts are available, it synchronizes the contact pair
     * archive and, when still empty, prompts the user to send an
     * invitation.
     */
    suspend fun selectContactButtonTapped() {
        val service = dependencies.recipientBar()
        service.layout.setIsSelectContactButtonEnabled(true)

        try {
            if (dependencies.contactPermissionStatus() != PermissionService.PermissionStatus.GRANTED) {
                if (!requestContactPermission()) return
                return selectContactButtonTapped()
            } else if (!dependencies.hasContactsBesidesCurrentUser()) {
                service.layout.setIsSelectContactButtonEnabled(false)
                dependencies.showProgressHUD()

                val exception =
                    try {
                        dependencies.syncContactPairArchive()
                        null
                    } catch (exception: Exception) {
                        exception
                    }

                if (exception != null && !exception.isEqual(to = AppException.mismatchedHashAndCallingCode)) {
                    Logger.log(exception, with = AlertType.toast)
                } else if (dependencies.hasContactsBesidesCurrentUser()) {
                    service.tableView.resolveContactPairs()
                    return selectContactButtonTapped()
                } else {
                    dependencies.presentInvitationSuggestionPrompt()
                    service.layout.setIsSelectContactButtonEnabled(true)
                    return
                }
            }

            dependencies.navigate(Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.ContactSelector)))
        } finally {
            dependencies.hideHUD()
        }
    }

    /**
     * Handles a change to the text field by filtering the contact
     * suggestions with its contents.
     *
     * @param text The text field's text.
     */
    fun textFieldChanged(text: String) {
        val service = dependencies.recipientBar()
        service.layout.setTextFieldText(text)
        service.tableView.setQuery(text)
    }

    // MARK: - Text Field Should Return

    /**
     * Handles the text field's return by adding the entered text as a
     * recipient.
     *
     * A phone number resolves to its registered user when one exists;
     * any other text is added as an unregistered recipient. When the
     * text is blank, the method instead deselects any provisional
     * recipients and returns focus to the message input bar.
     *
     * @param text The text entered in the text field.
     * @param makeInputBarFirstResponder A Boolean value that determines
     *   whether to make the message input bar the first responder when
     *   the text is blank.
     */
    suspend fun textFieldShouldReturn(
        text: String,
        makeInputBarFirstResponder: Boolean = true,
    ) {
        val service = dependencies.recipientBar()
        val contactSelectionUIService = service.contactSelectionUI

        if (text.isBlank()) {
            if (contactSelectionUIService.selectedContactPairs.value.none { it.isMock }) {
                contactSelectionUIService.unhighlightAllViews()
                if (!makeInputBarFirstResponder) return
                service.layout.inputBarBecomeFirstResponder()
                return
            }

            contactSelectionUIService.unhighlightAllViews()
            contactSelectionUIService.deselectMockContactPairs()
            Task.delayed(by = ActionHandlerFloats.BECOME_FIRST_RESPONDER_DELAY_MILLISECONDS.milliseconds) {
                service.layout.inputBarBecomeFirstResponder()
            }
            return
        }

        // A number keeps only dashes and pluses between its digits; inner
        // spaces make the entry a name.
        val phoneNumber = PhoneNumber(text)
        val sanitizedLength =
            text
                .replace("-", "")
                .replace("+", "")
                .trim()
                .length
        if (phoneNumber.compiledNumberString.length <= 1 || text.digits.length != sanitizedLength) {
            contactSelectionUIService.selectContactPair(ContactPair.mock(withName = text))
            return
        }

        try {
            val user = dependencies.getUser(phoneNumber)
            val contactPair = user.contactPair
            if (contactPair == null) {
                contactSelectionUIService.selectContactPair(ContactPair.withUser(user))
                return
            }

            contactSelectionUIService.selectContactPair(contactPair)
        } catch (_: Exception) {
            contactSelectionUIService.selectContactPair(ContactPair.mock(withName = text))
        }
    }

    // MARK: - Auxiliary

    /**
     * Requests the contacts permission, presenting the call to action
     * on denial and syncing the archive on grant.
     *
     * @return `true` if the permission was granted and the archive
     *   synced; otherwise, `false`.
     */
    private suspend fun requestContactPermission(): Boolean {
        try {
            val status = dependencies.requestContactPermission()
            if (status != PermissionService.PermissionStatus.GRANTED) {
                presentCTA()
                return false
            }

            try {
                dependencies.syncContactPairArchive()
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }

            return true
        } catch (exception: Exception) {
            if (exception.isEqual(to = AppException.contactAccessDenied)) {
                presentCTA()
                return false
            }

            Logger.log(exception, with = AlertType.toast)
            return false
        }
    }

    private fun presentCTA() {
        Task.delayed(by = ActionHandlerFloats.PRESENT_CONTACT_CTA_DELAY_MILLISECONDS.milliseconds) {
            dependencies.presentContactCTA()
        }
    }

    // MARK: - Companion

    private companion object {
        val HUD_HIDE_DELAY = 1.seconds
    }
}
