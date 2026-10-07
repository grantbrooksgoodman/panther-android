//
//  ConversationsPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.bundle.Application
import us.neotechnica.panther.bundle.conversation
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.developermode.DangerZone
import us.neotechnica.panther.bundle.reloadingConversationIDKeys
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.modules.common.services.InviteService
import us.neotechnica.panther.modules.common.services.KeyboardService
import us.neotechnica.panther.modules.common.services.PushTokenService
import us.neotechnica.panther.modules.common.services.ReviewService
import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.modules.content.user.models.MessageDeliveryServiceEffectID
import us.neotechnica.panther.modules.session.entity.extensions.conversations
import us.neotechnica.panther.modules.session.entity.extensions.filteredAndSorted
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

/**
 * Drives the conversations page's lifecycle, prompts, and data
 * reloads.
 *
 * Responds to the page's lifecycle and interactions: starts observing
 * current-user changes, evaluates startup prompts, reloads
 * conversation data on pull-to-refresh, and keeps the open chat page
 * consistent with session store changes.
 */
object ConversationsPageViewService {
    // MARK: - Types

    private enum class ReloadType {
        // Force update a shuffled third of conversations.
        FULL,

        // No force updating.
        MINIMAL,

        // Force update the most recent conversation.
        PARTIAL,
        ;

        val next: ReloadType
            get() =
                when (this) {
                    FULL -> PARTIAL
                    MINIMAL -> FULL
                    PARTIAL -> MINIMAL
                }
    }

    // MARK: - Properties

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentReloadType = ReloadType.FULL
    private var didShowSecondsToLoadToast = false

    // MARK: - View Lifecycle

    /**
     * Responds to the conversations page appearing by observing
     * current-user changes and registering the device's push token.
     */
    fun viewAppeared() {
        didShowSecondsToLoadToast = false
        UserSessionService.startObservingCurrentUserChanges()
        serviceScope.launch {
            try {
                PushTokenService.updatePushTokensForCurrentUser()
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }
    }

    /** Responds to the conversations page disappearing. */
    fun viewDisappeared() = Unit

    /**
     * Responds to the conversations page finishing its initial load by
     * evaluating the startup prompt flow after a short delay.
     */
    fun viewLoaded() {
        Task.delayed(by = 1.seconds) { showPromptsIfNeeded() }
    }

    /**
     * Reapplies the page's appearance after a trait collection change.
     *
     * If the chat page is presented, the update waits until it
     * closes.
     */
    fun traitCollectionChanged() {
        if (!ChatPageStateService.isPresented) return
        ChatPageStateService.addEffectUponIsPresented(
            state = false,
            id = ChatPageStateServiceEffectID.updateAppearance,
        ) {
            val sharedEvents = DependencyValues.current.sharedEvents
            sharedEvents.traitCollectionChanged.send(Unit)
        }
    }

    // MARK: - Reducer Action Handlers

    /**
     * Performs the developer-mode conversation deletion action.
     *
     * The current user's conversations are resolved before the action
     * performs; failures surface as a toast.
     */
    fun deleteConversationsToolbarButtonTapped() {
        serviceScope.launch {
            try {
                UserSessionService.resolveCurrentUser(
                    setOf(
                        UserSessionService.DataType.CONVERSATIONS,
                        UserSessionService.DataType.MESSAGES,
                    ),
                )
                DangerZone.deleteConversationsAction.perform()
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
    }

    /**
     * Keeps the open chat page consistent with a session store change.
     *
     * If a message is being sent, handling waits until the send
     * completes. Otherwise, this method marks unread messages as read
     * – updating the app badge – and notifies observers that the
     * current conversation's metadata changed. If the chat page is not
     * presented, this method does nothing.
     */
    fun handleChatPageStoreChange() {
        if (!ChatPageStateService.isPresented) return
        if (MessageDeliveryService.isSendingMessage.value) {
            Logger.log(
                "Awaiting message send completion before handling chat page store change...",
                domain = LoggerDomain.conversation,
            )
            MessageDeliveryService.addEffectUponIsSendingMessage(
                state = false,
                id = MessageDeliveryServiceEffectID.updateConversations,
            ) { handleChatPageStoreChange() }
            return
        }

        serviceScope.launch {
            ConversationSessionService.currentConversation ?: return@launch
            try {
                ReadReceiptService.updateReadDateForUnreadMessages()
            } catch (exception: Exception) {
                Logger.log(exception)
            }
            val sharedEvents = DependencyValues.current.sharedEvents
            sharedEvents.currentConversationMetadataChanged.send(Unit)
        }
    }

    /**
     * Reloads conversation data in response to a pull-to-refresh.
     *
     * Successive refreshes rotate through three reload depths; every
     * refresh re-resolves the current user's data.
     *
     * @throws Exception if resolving the user's data fails.
     */
    suspend fun reloadData() {
        try {
            reloadData(currentReloadType)
        } finally {
            DependencyValues.current.sharedStates.reloadingConversationIDKeys.value = emptySet()
        }
    }

    // MARK: - Auxiliary

    /** Logs how long the app's content took to load, once per page appearance. */
    fun showSecondsToLoadToastIfNeeded() {
        if (didShowSecondsToLoadToast) return
        didShowSecondsToLoadToast = true

        Task.debounced(TASK_ID_SHOW_SECONDS_TO_LOAD_TOAST, delay = 1.seconds) {
            val elapsedSeconds = abs(System.currentTimeMillis() - Application.loadStartDate.time) / MILLIS_PER_SECOND
            val secondsToLoad = max(elapsedSeconds - 1, 0L)
            val suffix = if (secondsToLoad == 1L) "" else "s"
            Logger.log(
                "Loaded content in $secondsToLoad second$suffix.",
                domain = LoggerDomain.conversation,
            )
        }
    }

    private suspend fun reloadData(type: ReloadType) {
        val conversations = UserSessionService.currentUser?.conversations?.filteredAndSorted
        if (!conversations.isNullOrEmpty() && (type == ReloadType.FULL || type == ReloadType.PARTIAL)) {
            val conversationsToReload =
                if (type == ReloadType.FULL) {
                    if (conversations.size > FULL_RELOAD_SHUFFLE_THRESHOLD) {
                        conversations.shuffled().take(conversations.size / FULL_RELOAD_FRACTION_DENOMINATOR + 1)
                    } else {
                        conversations
                    }
                } else {
                    listOf(conversations.first())
                }
            DependencyValues.current.sharedStates.reloadingConversationIDKeys.value =
                conversationsToReload.map { it.id.key }.toSet()
        }

        try {
            UserSessionService.resolveCurrentUser(UserSessionService.DataType.entries.toSet())
        } finally {
            currentReloadType = currentReloadType.next
        }
    }

    private suspend fun showPromptsIfNeeded() {
        // Dismiss the keyboard before presenting a startup prompt over the page.
        KeyboardService.resignFirstResponders()
        if (!InviteService.suggestInvitationIfNeeded()) {
            ReviewService.promptToReview()
        }
    }

    // MARK: - Companion

    private const val FULL_RELOAD_FRACTION_DENOMINATOR = 3
    private const val FULL_RELOAD_SHUFFLE_THRESHOLD = 5
    private const val MILLIS_PER_SECOND = 1000L
    private const val TASK_ID_SHOW_SECONDS_TO_LOAD_TOAST = "ConversationsPageViewService/showSecondsToLoadToast"
}
