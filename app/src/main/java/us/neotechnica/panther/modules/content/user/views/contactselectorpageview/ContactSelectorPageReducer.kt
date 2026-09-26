//
//  ContactSelectorPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 25/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.extensions.noUsersWithPhoneNumber
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.services.ContactSelectorPageViewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.localization.models.localized
import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.networking.user.models.User
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
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
 *
 * @param onSelectRecipient Invoked with a selection's user id and
 *   display name when presented from the new chat page.
 */
class ContactSelectorPageReducer(
    private val onSelectRecipient: (userID: String, displayName: String) -> Unit = { _, _ -> },
) : Reducer<ContactSelectorPageReducer.State, ContactSelectorPageReducer.Action> {
    // MARK: - Types

    /** The context the page was presented from. */
    enum class EntryPoint {
        CHAT_INFO_PAGE_VIEW,
        NEW_CHAT_PAGE_VIEW,
    }

    // MARK: - Action

    sealed interface Action {
        data object ViewAppeared : Action

        data object CancelToolbarButtonTapped : Action

        data object FindUserButtonTapped : Action

        data object InviteToolbarButtonTapped : Action

        data class FindUserFailed(
            val exception: Exception,
        ) : Action

        data class FindUserReturned(
            val user: User,
        ) : Action

        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action

        data class SearchQueryChanged(
            val searchQuery: String,
        ) : Action

        data class SelectedContactPairChanged(
            val selectedContactPair: ContactMatch,
        ) : Action
    }

    // MARK: - State

    data class State(
        val entryPoint: EntryPoint,
        val inviteToolbarButtonText: String = LocalizedStringKey.Invite.localized(),
        val searchQuery: String = "",
        val selectedContactPair: ContactMatch? = null,
        val strings: List<TranslationOutputMap> = ContactSelectorPageViewStrings.defaultOutputMap,
        val viewState: ViewState = ViewState.Loading,
        val foundContactPair: ContactMatch? = null,
    ) {
        /** The user's known contacts. */
        val contactPairs: List<ContactMatch>
            get() = ContactService.matches()

        /** The page's navigation title. */
        val navigationTitle: String
            get() =
                if (entryPoint == EntryPoint.CHAT_INFO_PAGE_VIEW) {
                    strings.value(ContactSelectorPageViewStrings.navigationTitle)
                } else {
                    LocalizedStringKey.Contacts.localized()
                }

        /** The text shown when no contacts match the search. */
        val noResultsLabelText: String
            get() {
                if (entryPoint == EntryPoint.CHAT_INFO_PAGE_VIEW && searchQuery.isNotBlank() && searchQuery == searchQuery.digits) {
                    return strings.value(ContactSelectorPageViewStrings.noResultsLabelText)
                }
                return LocalizedStringKey.NoResults.localized()
            }

        /** The contacts matching the search query, or the single found user when a lookup succeeded. */
        val queriedContactPairs: List<ContactMatch>
            get() = foundContactPair?.let { listOf(it) } ?: contactPairs.queried(searchQuery)

        /** The search bar's placeholder text. */
        val searchBarPlaceholderText: String
            get() =
                if (entryPoint == EntryPoint.CHAT_INFO_PAGE_VIEW) {
                    strings.value(ContactSelectorPageViewStrings.searchBarPlaceholderText)
                } else {
                    LocalizedStringKey.Search.localized()
                }

        /** The queried contacts grouped into sections by their section title. */
        val sections: Map<String, List<ContactMatch>>
            get() = queriedContactPairs.groupBy { sectionTitle(it.fullName) }

        /** Whether the invite button is shown. */
        val shouldShowInviteButton: Boolean
            get() = contactPairs.isEmpty() || entryPoint == EntryPoint.NEW_CHAT_PAGE_VIEW

        val queryMatchesFoundContactPair: Boolean
            get() {
                val found = foundContactPair ?: return false
                return listOf(found.compiledNumberString.digits, found.nationalNumberString.digits).contains(searchQuery)
            }
    }

    // MARK: - Reduce

    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                if (state.entryPoint != EntryPoint.CHAT_INFO_PAGE_VIEW) {
                    ReduceResult(state.copy(viewState = ViewState.Loaded))
                } else {
                    ReduceResult(state.copy(viewState = ViewState.Loading), resolveEffect())
                }

            Action.CancelToolbarButtonTapped -> {
                ContactSelectorPageViewService.cancelToolbarButtonTapped(from = state.entryPoint)
                ReduceResult(state)
            }

            Action.FindUserButtonTapped ->
                if (state.entryPoint == EntryPoint.CHAT_INFO_PAGE_VIEW &&
                    state.queriedContactPairs.isEmpty() &&
                    state.searchQuery == state.searchQuery.digits
                ) {
                    ReduceResult(state, findUserEffect(phoneNumber(state.searchQuery.digits)))
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
                    ReduceResult(state, invitationPromptEffect(phoneNumber(state.searchQuery.digits)))
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
                        ContactSelectorPageViewService.selectedContactPairChanged(
                            selected,
                            from = entryPoint,
                            onSelectForNewChat = onSelectRecipient,
                        )
                    },
                )
            }
        }

    // MARK: - Auxiliary

    private fun foundContactPair(user: User): ContactMatch =
        ContactService.match(user.id)
            ?: ContactMatch(
                userID = user.id,
                fullName = user.phoneNumber.formattedString(),
                compiledNumberString = user.phoneNumber.compiledNumberString,
                nationalNumberString = user.phoneNumber.nationalNumberString,
            )

    private fun phoneNumber(digits: String): PhoneNumber {
        val regionCode = RegionDetailService.deviceRegionCode
        return PhoneNumber(
            callingCode = RegionDetailService.callingCode(regionCode) ?: PhoneNumberService.deviceCallingCode,
            nationalNumberString = digits,
            regionCode = regionCode,
            label = null,
            internalFormattedString = null,
        )
    }

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(Networking.config.hostedTranslationDelegate.resolve(ContactSelectorPageViewStrings)))
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

// MARK: - Contact Matching

private fun List<ContactMatch>.queried(searchQuery: String): List<ContactMatch> {
    if (searchQuery.isBlank()) return this
    val normalized = searchQuery.trim().lowercase()
    return filter { it.fullName.lowercase().contains(normalized) || it.compiledNumberString.contains(searchQuery.digits) }
}

private fun sectionTitle(name: String): String {
    val firstLetter = name.trim().firstOrNull { it.isLetter() } ?: return "#"
    return firstLetter.uppercase()
}
