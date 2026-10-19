//
//  ContactSelectorPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.noUsersWithPhoneNumber
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.extensions.compiledNumberStrings
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.content.user.extensions.contactPair
import us.neotechnica.panther.modules.content.user.extensions.displayName
import us.neotechnica.panther.modules.content.user.extensions.queried
import us.neotechnica.panther.modules.content.user.extensions.tableViewSectionTitle
import us.neotechnica.panther.modules.content.user.extensions.users
import us.neotechnica.panther.modules.content.user.extensions.withUser
import us.neotechnica.panther.modules.content.user.services.ContactSelectorPageViewService
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult

/**
 * The reducer that drives the contact selector page.
 *
 * This page lets the user choose a contact – either to add a
 * participant to an existing conversation, when presented from the chat
 * info page, or to start a new conversation, when presented from the new
 * chat page. Its title, strings, and available actions depend on the
 * entry point it was presented from.
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings when
 *   presented from the chat info page; from the new chat page, it loads
 *   immediately. If resolution fails, the page loads anyway.
 * - The contact list shows the user's known contacts filtered by the
 *   search query, grouped into alphabetical sections.
 * - When presented from the chat info page and the query is a phone
 *   number with no matching contact, the user can look up a registered
 *   user by that number. A successful lookup shows the user as the sole
 *   result; a failed lookup offers to send an invitation.
 * - Selecting a contact, cancelling, and inviting someone are performed
 *   through [ContactSelectorPageViewService].
 */
class ContactSelectorPageReducer : Reducer<ContactSelectorPageReducer.State, ContactSelectorPageReducer.Action> {
    // MARK: - Dependencies

    private val translator: HostedTranslationDelegate by Dependency { it.networking.hostedTranslation }

    // MARK: - Actions

    /** The actions the contact selector page can process. */
    sealed interface Action {
        /** An action that indicates the view appeared. Begins display string resolution when presented from the chat info page. */
        data object ViewAppeared : Action

        /** An action that indicates the view disappeared. */
        data object ViewDisappeared : Action

        /** An action that indicates the user tapped the cancel toolbar button. Dismisses the page. */
        data object CancelToolbarButtonTapped : Action

        /** An action that indicates the user tapped the find-user button. Looks up a registered user by the entered phone number. */
        data object FindUserButtonTapped : Action

        /** An action that indicates the user tapped the invite toolbar button. Begins inviting someone to the app. */
        data object InviteToolbarButtonTapped : Action

        /** An action that indicates the user lookup failed, carrying the resulting [Exception]. */
        data class FindUserFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates the user lookup succeeded, carrying the found user. Shows the user as the only result. */
        data class FindUserReturned(
            val user: User,
        ) : Action

        /** An action that indicates display string resolution failed, carrying the resulting [Exception]. */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /** An action that indicates display string resolution succeeded, carrying the resolved strings. */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        /** An action that indicates the search query changed, carrying the new query. */
        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        /** An action that indicates the user selected a contact pair, carrying it. */
        data class SelectedContactPairChanged(
            val selectedContactPair: ContactPair,
        ) : Action
    }

    // MARK: - State

    /** The state of the contact selector page. */
    data class State(
        /** The context the page was presented from. */
        val entryPoint: ContactSelectorPageView.EntryPoint,

        /** The localized text the invite toolbar button displays. */
        val inviteToolbarButtonText: String = LocalizedStringKey.Invite.localized(),

        /** The search query the user has entered. */
        val searchQuery: String = "",

        /** The contact pair the user selected, or `null` if none is selected. */
        val selectedContactPair: ContactPair? = null,

        /** The page's translated display strings. */
        val strings: List<TranslationOutputMap> = ContactSelectorPageViewStrings.defaultOutputMap,

        /** The page's loading state. */
        val viewState: ViewState = ViewState.Loading,
        internal val foundContactPair: ContactPair? = null,
    ) {
        /** The user's known contact pairs, from the contact pair archive. */
        val contactPairs: List<ContactPair>
            get() =
                DependencyValues.current.commonServices.contact.contactPairArchive
                    .allValues()

        /** The page's navigation title. */
        val navigationTitle: String
            get() =
                if (entryPoint == ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW) {
                    strings.value(ContactSelectorPageViewStrings.navigationTitle, ContactSelectorPageViewStrings)
                } else {
                    LocalizedStringKey.Contacts.localized()
                }

        /** The text shown when no contacts match the search. */
        val noResultsLabelText: String
            get() {
                val isPhoneNumberQuery = searchQuery.isNotBlank() && searchQuery == searchQuery.digits
                if (entryPoint == ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW && isPhoneNumberQuery) {
                    return strings.value(ContactSelectorPageViewStrings.noResultsLabelText, ContactSelectorPageViewStrings)
                }
                return LocalizedStringKey.NoResults.localized()
            }

        /** The contact pairs matching the search query, or the single found user when a lookup succeeded. */
        val queriedContactPairs: List<ContactPair>
            get() = foundContactPair?.let { listOf(it) } ?: contactPairs.queried(searchQuery)

        /** The search bar's placeholder text. */
        val searchBarPlaceholderText: String
            get() =
                if (entryPoint == ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW) {
                    strings.value(ContactSelectorPageViewStrings.searchBarPlaceholderText, ContactSelectorPageViewStrings)
                } else {
                    LocalizedStringKey.Search.localized()
                }

        /** The queried contact pairs grouped into sections by their section title. */
        val sections: Map<String, List<ContactPair>>
            get() = queriedContactPairs.groupBy { it.contact.tableViewSectionTitle }

        /** Whether the invite button is shown. */
        val shouldShowInviteButton: Boolean
            get() = contactPairs.isEmpty() || entryPoint == ContactSelectorPageView.EntryPoint.NEW_CHAT_PAGE_VIEW

        internal val queryMatchesFoundContactPair: Boolean
            get() {
                val found = foundContactPair ?: return false
                val phoneNumbers = found.users.map { it.phoneNumber }
                val numberStrings = (phoneNumbers.compiledNumberStrings + phoneNumbers.map { it.nationalNumberString }).map { it.digits }
                return numberStrings.contains(searchQuery)
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                if (state.entryPoint != ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW) {
                    ReduceResult(state.copy(viewState = ViewState.Loaded))
                } else {
                    ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())
                }

            Action.ViewDisappeared -> ReduceResult(state)

            Action.CancelToolbarButtonTapped -> {
                ContactSelectorPageViewService.cancelToolbarButtonTapped(from = state.entryPoint)
                ReduceResult(state)
            }

            Action.FindUserButtonTapped ->
                if (state.entryPoint == ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW &&
                    state.queriedContactPairs.isEmpty() &&
                    state.searchQuery == state.searchQuery.digits
                ) {
                    ReduceResult(state, findUserEffect(PhoneNumber(state.searchQuery.digits)))
                } else {
                    ReduceResult(state)
                }

            Action.InviteToolbarButtonTapped -> {
                ContactSelectorPageViewService.inviteToolbarButtonTapped()
                ReduceResult(state)
            }

            is Action.FindUserReturned ->
                ReduceResult(state.copy(foundContactPair = foundContactPair(action.user)))

            is Action.FindUserFailed ->
                if (!action.exception.isEqual(to = AppException.noUsersWithPhoneNumber)) {
                    Logger.log(action.exception, with = AlertType.toast)
                    ReduceResult(state)
                } else {
                    ReduceResult(state, invitationPromptEffect(PhoneNumber(state.searchQuery.digits)))
                }

            is Action.ResolveFailed -> {
                Logger.log(action.exception, with = AlertType.toast)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))

            is Action.SearchQueryChanged -> {
                val updated = state.copy(searchQuery = action.searchQuery)
                if (action.searchQuery.isBlank() || !updated.queryMatchesFoundContactPair) {
                    ReduceResult(updated.copy(foundContactPair = null))
                } else {
                    ReduceResult(updated)
                }
            }

            is Action.SelectedContactPairChanged -> {
                val entryPoint = state.entryPoint
                val selected = action.selectedContactPair
                ReduceResult(
                    state.copy(selectedContactPair = selected),
                    Effect.fireAndForget {
                        ContactSelectorPageViewService.selectedContactPairChanged(selected, from = entryPoint)
                    },
                )
            }
        }

    // MARK: - Auxiliary

    private fun foundContactPair(user: User): ContactPair = user.contactPair ?: ContactPair.withUser(user, name = user.displayName)

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(translator.resolve(ContactSelectorPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }

    private fun findUserEffect(phoneNumber: PhoneNumber): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.FindUserReturned(ContactSelectorPageViewService.findUser(with = phoneNumber)))
            } catch (exception: Exception) {
                send(Action.FindUserFailed(exception))
            }
        }

    private fun invitationPromptEffect(phoneNumber: PhoneNumber): Effect<Action> =
        Effect.fireAndForget {
            ContactSelectorPageViewService.presentInvitationPrompt(phoneNumber)
        }
}
