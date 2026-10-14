//
//  SettingsPageReducer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.settingspageview

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.navigators.UserContentRoute
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.components.ListRowView
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.content.user.dependencies.settingsPageViewService
import us.neotechnica.panther.modules.content.user.models.BuildInfoButtonStrings
import us.neotechnica.panther.modules.content.user.services.SettingsPageViewService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.extensions.isBangQualifiedEmpty
import us.neotechnica.panther.networking.modules.translation.extensions.value
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.effect.Effect
import us.neotechnica.panther.subsystem.modules.effect.merge
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.reducer.interfaces.Reducer
import us.neotechnica.panther.subsystem.modules.reducer.models.ReduceResult
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import java.util.UUID

/**
 * The reducer that drives the settings page.
 *
 * This page presents the app's settings. It shows the user's contact
 * card and provides the buttons for the app's options – blocked users,
 * language, and account actions. Most of these actions are performed
 * through [SettingsPageViewService].
 *
 * The page's behavior contract:
 *
 * - On appearance, the page resolves its translated display strings and
 *   the user's contact card concurrently, and remains in the loading
 *   state until string resolution completes.
 * - Tapping a button performs its action, and tapping done dismisses
 *   the page.
 */
class SettingsPageReducer : Reducer<SettingsPageReducer.State, SettingsPageReducer.Action> {
    // MARK: - Dependencies

    private val translator: HostedTranslationDelegate by Dependency { it.networking.hostedTranslation }
    private val userSession: UserSessionService by Dependency { it.clientSession.entity.user }
    private val viewService: SettingsPageViewService by Dependency { it.settingsPageViewService }

    // MARK: - Actions

    /** The actions the settings page can process. */
    sealed interface Action {
        /**
         * An action that indicates the view appeared. Begins resolving
         * the display strings and the user's contact card.
         */
        data object ViewAppeared : Action

        /**
         * An action that indicates the view disappeared. Notifies
         * observers of a trait collection change that occurred while
         * the page was presented.
         */
        data object ViewDisappeared : Action

        /**
         * An action that indicates the user tapped the blocked users
         * button. Presents the blocked users list.
         */
        data object BlockedUsersButtonTapped : Action

        /**
         * An action that indicates the user tapped the build info
         * button. Advances the build info to its next value.
         */
        data object BuildInfoButtonTapped : Action

        /**
         * An action that indicates the user tapped the clear caches
         * button. Clears the app's caches.
         */
        data object ClearCachesButtonTapped : Action

        /**
         * An action that indicates the user tapped the delete account
         * button. Begins deleting the account.
         */
        data object DeleteAccountButtonTapped : Action

        /**
         * An action that indicates the user tapped the done button.
         * Dismisses the page.
         */
        data object DoneToolbarButtonTapped : Action

        /**
         * An action that indicates the user tapped the invite friends
         * button. Begins inviting friends to the app.
         */
        data object InviteFriendsButtonTapped : Action

        /**
         * An action that indicates the user tapped the leave review
         * button. Prompts the user to leave a review.
         */
        data object LeaveReviewButtonTapped : Action

        /**
         * An action that indicates a long press on the build info
         * button was recognized. Copies the build info to the
         * clipboard, or – after the copyright text has been revealed
         * repeatedly – offers to enter prerelease mode.
         */
        data object LongPressGestureRecognized : Action

        /**
         * An action that indicates the user tapped the send feedback
         * button. Begins sending feedback.
         */
        data object SendFeedbackButtonTapped : Action

        /**
         * An action that indicates the user tapped the sign out button.
         * Signs the user out.
         */
        data object SignOutButtonTapped : Action

        /** An action that indicates the trait collection changed. Rebuilds the page. */
        data object TraitCollectionChanged : Action

        /**
         * An action that indicates resolving the user's contact card
         * failed, carrying the resulting [Exception]. Falls back to the
         * user's phone number.
         */
        data class FetchCNContactForCurrentUserFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates the user's contact card resolved,
         * carrying the contact. Updates the contact detail view.
         */
        data class FetchCNContactForCurrentUserReturned(
            val contactPair: ContactPair,
        ) : Action

        /**
         * An action that indicates display string resolution failed,
         * carrying the resulting [Exception].
         */
        data class ResolveFailed(
            val exception: Exception,
        ) : Action

        /**
         * An action that indicates display string resolution succeeded,
         * carrying the resolved strings.
         */
        data class ResolveReturned(
            val strings: List<TranslationOutputMap>,
        ) : Action
    }

    // MARK: - State

    /**
     * The state of the settings page.
     *
     * @property buildInfoButtonStrings The strings the build info button
     *   currently displays.
     * @property contactDetailViewImage The image shown in the user's
     *   contact detail view.
     * @property contactDetailViewSubtitleLabelText The subtitle shown in
     *   the user's contact detail view.
     * @property contactDetailViewTitleLabelText The title shown in the
     *   user's contact detail view.
     * @property contactPair The current user's device contact, if
     *   available.
     * @property developerModeListItems The developer mode list rows, if
     *   any.
     * @property groupedListViewsID The identity of the grouped list.
     *   Regenerated to rebuild it when the trait collection changes.
     * @property strings The page's translated display strings. Contains
     *   the default, untranslated strings until resolution completes.
     * @property viewID The identity of the page's content. Regenerated
     *   to rebuild it from the latest values.
     * @property viewState The page's loading state. Remains loading
     *   until display string resolution completes.
     */
    data class State(
        val buildInfoButtonStrings: BuildInfoButtonStrings =
            BuildInfoButtonStrings(BuildInfoButtonStrings.BuildInfoButtonStringKey.BUNDLE_VERSION_AND_BUILD_NUMBER),
        val contactDetailViewImage: Bitmap? = null,
        val contactDetailViewSubtitleLabelText: String? = null,
        val contactDetailViewTitleLabelText: String = "",
        val contactPair: ContactPair? = null,
        val developerModeListItems: List<ListRowView.Configuration>? = null,
        val groupedListViewsID: UUID = UUID.randomUUID(),
        val strings: List<TranslationOutputMap> = SettingsPageViewStrings.defaultOutputMap,
        val viewID: UUID = UUID.randomUUID(),
        val viewState: ViewState = ViewState.Loading,
        internal val timesEncounteredCopyrightText: Int = 0,
        internal val traitCollectionChanged: Boolean = false,
    ) {
        /** The localized text the done button displays. */
        val doneToolbarButtonText: String
            get() = LocalizedStringKey.Done.localized(LocalizationSource.SUBSYSTEM)

        /** The page's navigation title. */
        val navigationTitle: String
            get() = LocalizedStringKey.Settings.localized(LocalizationSource.SUBSYSTEM).replace("…", "")

        /** The blocked users button's text, including the count of blocked users. */
        val blockedUsersButtonText: String
            get() {
                val count =
                    (
                        DependencyValues.current.clientSession.entity.user.currentUser
                            ?.blockedUserIDs ?: emptyList()
                    ).count { !it.isBangQualifiedEmpty }
                return "${strings.value(SettingsPageViewStrings.blockedUsersButtonText)} ($count)"
            }

        /** The build info button's image for dark backgrounds. */
        val buildInfoButtonDarkBackgroundImage: Int
            @DrawableRes get() = R.drawable.nt_white

        /** The build info button's image for light backgrounds. */
        val buildInfoButtonLightBackgroundImage: Int
            @DrawableRes get() = R.drawable.nt_black

        /**
         * A Boolean value that indicates whether the blocked users
         * button is enabled. Enabled when the user has blocked at
         * least one user.
         */
        val isBlockedUsersButtonEnabled: Boolean
            get() =
                !(
                    DependencyValues.current.clientSession.entity.user.currentUser
                        ?.blockedUserIDs ?: emptyList()
                ).isBangQualifiedEmpty
    }

    // MARK: - Reduce

    // The counterpart reducer carries `// swiftlint:disable function_body_length`; the
    // cyclomatic suppression covers the action `when`, whose branches STYLE_RULES §16 exempts.
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun reduce(
        state: State,
        action: Action,
    ): ReduceResult<State, Action> =
        when (action) {
            Action.ViewAppeared ->
                ReduceResult(
                    state.copy(
                        developerModeListItems = viewService.developerModeListItems(),
                        viewState = ViewState.Loading,
                    ),
                    Effect.merge(resolveEffect(), fetchCNContactForCurrentUserEffect()),
                )

            Action.ViewDisappeared -> {
                if (state.traitCollectionChanged) {
                    DependencyValues.current.sharedEvents.traitCollectionChanged
                        .send(Unit)
                }
                ReduceResult(state)
            }

            Action.BlockedUsersButtonTapped -> {
                viewService.blockedUsersButtonTapped()
                ReduceResult(state)
            }

            Action.BuildInfoButtonTapped -> {
                val next = state.buildInfoButtonStrings.next
                val isCopyright = next.key == BuildInfoButtonStrings.BuildInfoButtonStringKey.COPYRIGHT
                ReduceResult(
                    state.copy(
                        buildInfoButtonStrings = next,
                        timesEncounteredCopyrightText = state.timesEncounteredCopyrightText + if (isCopyright) 1 else 0,
                    ),
                )
            }

            Action.ClearCachesButtonTapped -> {
                viewService.clearCachesButtonTapped()
                ReduceResult(state)
            }

            Action.DeleteAccountButtonTapped -> {
                viewService.deleteAccountButtonTapped()
                ReduceResult(state)
            }

            Action.DoneToolbarButtonTapped -> {
                DependencyValues.current.navigation.navigate(Route.UserContent(UserContentRoute.Pop))
                ReduceResult(state)
            }

            Action.InviteFriendsButtonTapped -> {
                viewService.inviteFriendsButtonTapped()
                ReduceResult(state)
            }

            Action.LeaveReviewButtonTapped -> {
                viewService.leaveReviewButtonTapped()
                ReduceResult(state)
            }

            Action.LongPressGestureRecognized -> {
                if (state.buildInfoButtonStrings.key == BuildInfoButtonStrings.BuildInfoButtonStringKey.COPYRIGHT &&
                    state.timesEncounteredCopyrightText > 1
                ) {
                    viewService.promptToEnterPrereleaseMode()
                } else {
                    viewService.setClipboardWithHapticFeedback(state.buildInfoButtonStrings.labelText)
                }
                ReduceResult(state)
            }

            Action.SendFeedbackButtonTapped -> {
                viewService.sendFeedbackButtonTapped()
                ReduceResult(state)
            }

            Action.SignOutButtonTapped -> {
                viewService.signOutButtonTapped()
                ReduceResult(state)
            }

            Action.TraitCollectionChanged -> {
                val newState = state.copy(traitCollectionChanged = true, viewID = UUID.randomUUID())
                if (viewService.isMainPagePresented) {
                    ReduceResult(newState.copy(groupedListViewsID = UUID.randomUUID()))
                } else {
                    ReduceResult(newState)
                }
            }

            is Action.FetchCNContactForCurrentUserFailed -> {
                Logger.log(action.exception)
                val phoneNumber = userSession.currentUser?.phoneNumber?.formattedString()
                ReduceResult(
                    state.copy(contactDetailViewTitleLabelText = phoneNumber ?: state.contactDetailViewTitleLabelText),
                )
            }

            is Action.FetchCNContactForCurrentUserReturned -> {
                val contact = action.contactPair.contact
                val formattedPhoneNumberString =
                    userSession.currentUser?.phoneNumber?.formattedString()
                        ?: contact.phoneNumbers.firstOrNull()?.formattedString()
                ReduceResult(
                    state.copy(
                        contactDetailViewImage = contact.image,
                        contactDetailViewSubtitleLabelText =
                            if (formattedPhoneNumberString == contact.fullName) "" else formattedPhoneNumberString,
                        contactDetailViewTitleLabelText = contact.fullName,
                        contactPair = action.contactPair,
                    ),
                )
            }

            is Action.ResolveFailed -> {
                Logger.log(action.exception)
                ReduceResult(state.copy(viewState = ViewState.Loaded))
            }

            is Action.ResolveReturned ->
                ReduceResult(state.copy(strings = action.strings, viewState = ViewState.Loaded))
        }

    // MARK: - Auxiliary

    private fun fetchCNContactForCurrentUserEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.FetchCNContactForCurrentUserReturned(viewService.fetchCNContactForCurrentUser()))
            } catch (exception: Exception) {
                send(Action.FetchCNContactForCurrentUserFailed(exception))
            }
        }

    private fun resolveEffect(): Effect<Action> =
        Effect.run { send ->
            try {
                send(Action.ResolveReturned(translator.resolve(SettingsPageViewStrings)))
            } catch (exception: Exception) {
                send(Action.ResolveFailed(exception))
            }
        }
}
