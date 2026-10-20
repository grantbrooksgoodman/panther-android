//
//  ConversationsPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

@file:Suppress("FileLength")

package us.neotechnica.panther.modules.content.user.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import us.neotechnica.panther.Application
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.bundle.bugPrevention
import us.neotechnica.panther.bundle.conversation
import us.neotechnica.panther.bundle.currentConversationMetadataChanged
import us.neotechnica.panther.bundle.developermode.DangerZone
import us.neotechnica.panther.bundle.mismatchedHashAndCallingCode
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.bundle.notAuthorizedForContacts
import us.neotechnica.panther.bundle.reloadingConversationIDKeys
import us.neotechnica.panther.bundle.traitCollectionChanged
import us.neotechnica.panther.designsystem.modules.foundation.toast.Toast
import us.neotechnica.panther.modules.common.extensions.isOnline
import us.neotechnica.panther.modules.common.extensions.roundedString
import us.neotechnica.panther.modules.common.services.commonServices
import us.neotechnica.panther.modules.common.services.sources.ConnectionStatusService
import us.neotechnica.panther.modules.common.services.sources.PermissionService
import us.neotechnica.panther.modules.common.services.sources.PushTokenService
import us.neotechnica.panther.modules.common.services.sources.UpdateService
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.modules.content.user.extensions.hasContactsBesidesCurrentUser
import us.neotechnica.panther.modules.content.user.extensions.markStaleLocally
import us.neotechnica.panther.modules.content.user.models.ChatPageStateServiceEffectID
import us.neotechnica.panther.modules.content.user.models.ConnectionStatusServiceEffectID
import us.neotechnica.panther.modules.content.user.models.ConversationCellViewData
import us.neotechnica.panther.modules.content.user.models.MessageDeliveryServiceEffectID
import us.neotechnica.panther.modules.content.user.services.chatpageview.ChatPageViewService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.ReadReceiptService
import us.neotechnica.panther.modules.networking.sources.common.extensions.allDataTypes
import us.neotechnica.panther.modules.networking.sources.common.extensions.clearTemporaryCaches
import us.neotechnica.panther.modules.networking.sources.common.extensions.sortedByLatestMessageSentDate
import us.neotechnica.panther.modules.networking.sources.common.extensions.uniquedByID
import us.neotechnica.panther.modules.networking.sources.common.extensions.visibleForCurrentUser
import us.neotechnica.panther.modules.networking.sources.common.extensions.withGlobalCacheStrategy
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.networking.sources.user.models.User
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.UserSessionService
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.networking.modules.common.dependencies.networking
import us.neotechnica.panther.networking.modules.common.models.CacheStrategy
import us.neotechnica.panther.networking.modules.database.interfaces.DatabaseDelegate
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.AppException
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.LoggerDomain
import us.neotechnica.panther.subsystem.modules.foundation.models.ToastStyle
import us.neotechnica.panther.subsystem.modules.foundation.services.Build
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger.AlertType
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedEvents
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.SharedEvents
import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Drives the conversations page's lifecycle, prompts, and data
 * reloads.
 *
 * Responds to the conversations page's lifecycle and user
 * interactions: starts observing current-user changes, evaluates
 * startup prompts, reloads conversation data on pull-to-refresh, and
 * keeps the open chat page consistent with session store changes.
 *
 * @param dependencies The collaborators the service calls; the
 *   defaults resolve the production services.
 */
@Suppress("LargeClass")
class ConversationsPageViewService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Types

    /**
     * The collaborators the service calls.
     *
     * @property clearTemporaryCaches Clears the database's temporary
     *   caches.
     * @property currentConversation Resolves the session's current
     *   conversation.
     * @property currentUser Resolves the session's current user.
     * @property downloadVoiceIfNeeded Downloads a speech voice for
     *   the given language code if none is installed.
     * @property hasContactsBesidesCurrentUser Whether the contact
     *   archive holds anyone besides the current user.
     * @property isChatPagePresented Whether the chat page is
     *   presented.
     * @property isSendingMessage Whether a message is being sent.
     * @property lastContactSyncDate Reads the last contact sync date.
     * @property notificationPermissionStatus Reads the notifications
     *   permission status.
     * @property promptToReview Prompts the user for a store review.
     * @property requestNotificationPermission Requests the
     *   notifications permission.
     * @property resolveCurrentUser Re-resolves the current user's
     *   data.
     * @property resolveLastMessage Re-fetches a conversation's last
     *   message, returning cached values on failure.
     * @property setLastContactSyncDate Writes the last contact sync
     *   date.
     * @property suggestInvitationIfNeeded Suggests inviting friends,
     *   returning whether a suggestion was made.
     * @property syncContactPairArchive Syncs the contact pair
     *   archive.
     * @property updateReadDateForUnreadMessages Marks the current
     *   conversation's unread messages read, returning whether any
     *   were unread.
     */
    data class Dependencies(
        val clearTemporaryCaches: () -> Unit = {
            DependencyValues.current.networking.database
                .clearTemporaryCaches()
        },
        val currentConversation: () -> Conversation? = { DependencyValues.current.clientSession.entity.conversation.currentConversation },
        val currentUser: () -> User? = { DependencyValues.current.clientSession.entity.user.currentUser },
        val downloadVoiceIfNeeded: (String) -> Unit = {
            DependencyValues.current.commonServices.audio.textToSpeech
                .downloadVoiceIfNeeded(it)
        },
        val hasContactsBesidesCurrentUser: () -> Boolean = {
            DependencyValues.current.commonServices.contact.hasContactsBesidesCurrentUser
        },
        val isChatPagePresented: () -> Boolean = { ChatPageStateService.isPresented },
        val isNewChatPagePresented: () -> Boolean = {
            DependencyValues.current.navigation.state.value.userContent.stack
                .lastOrNull() == UserContentNavigatorState.SeguePaths.NewChat
        },
        val isSendingMessage: () -> Boolean = { MessageDeliveryService.isSendingMessage.value },
        val lastContactSyncDate: () -> Date? = { DependencyValues.current.commonServices.contact.contactPairArchive.lastContactSyncDate },
        val notificationPermissionStatus: () -> PermissionService.PermissionStatus = {
            DependencyValues.current.commonServices.permission.notificationPermissionStatus
        },
        val promptToReview: () -> Unit = {
            DependencyValues.current.commonServices.review
                .promptToReview()
        },
        val requestNotificationPermission: suspend () -> Unit = {
            DependencyValues.current.commonServices.permission
                .requestPermission(PermissionService.PermissionType.NOTIFICATIONS)
        },
        val resolveCurrentUser: suspend (
            Set<User.DataType>,
        ) -> Unit = {
            DependencyValues.current.clientSession.entity.user
                .resolveCurrentUser(it)
        },
        val resolveLastMessage: suspend (String, Conversation) -> Unit = { lastMessageID, conversation ->
            DependencyValues.current.networking.database.withGlobalCacheStrategy(CacheStrategy.RETURN_CACHE_ON_FAILURE) {
                conversation.resolveMessages(setOf(lastMessageID))
            }
        },
        val setLastContactSyncDate: (Date?) -> Unit = {
            DependencyValues.current.commonServices.contact.contactPairArchive.lastContactSyncDate =
                it
        },
        val suggestInvitationIfNeeded: suspend () -> Boolean = {
            DependencyValues.current.commonServices.invite
                .suggestInvitationIfNeeded()
        },
        val syncContactPairArchive: suspend () -> Unit = {
            DependencyValues.current.commonServices.contact
                .syncContactPairArchive()
        },
        val updateReadDateForUnreadMessages: suspend () -> Boolean = { ReadReceiptService.updateReadDateForUnreadMessages() },
    )

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

    // MARK: - Dependencies

    private val connectionStatusService: ConnectionStatusService by Dependency { it.commonServices.connectionStatus }
    private val database: DatabaseDelegate by Dependency { it.networking.database }
    private val pushTokenService: PushTokenService by Dependency { it.commonServices.pushToken }
    private val sessionStore: SessionStore by Dependency { it.clientSession.store }
    private val sharedEvents: SharedEvents by Dependency { it.sharedEvents }
    private val sharedStates: SharedStates by Dependency { it.sharedStates }
    private val updateService: UpdateService by Dependency { it.commonServices.update }
    private val userSession: UserSessionService by Dependency { it.clientSession.entity.user }

    // MARK: - Properties

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentReloadType = ReloadType.FULL
    private var didShowSecondsToLoadToast = false

    // MARK: - View Lifecycle

    /**
     * Responds to the conversations page appearing by observing
     * current-user changes and registering the device's push token
     * for the current user.
     */
    fun viewAppeared() {
        didShowSecondsToLoadToast = false
        userSession.startObservingCurrentUserChanges()
        serviceScope.launch {
            try {
                pushTokenService.updatePushTokensForCurrentUser()
            } catch (exception: Exception) {
                Logger.log(exception)
            }
        }
    }

    /** Responds to the conversations page disappearing. */
    fun viewDisappeared() = Unit

    /**
     * Responds to the conversations page finishing its initial load.
     *
     * Clears temporary database caches, downloads a speech voice for
     * the current user's language if none is installed, evaluates the
     * startup prompt flow after a short delay, and registers the
     * connection status effects that check for updates when
     * connectivity is restored and show an offline toast when it is
     * lost.
     */
    fun viewLoaded() {
        dependencies.clearTemporaryCaches()
        downloadVoiceIfNeeded()
        Task.delayed(by = PROMPT_DELAY) { showPromptsIfNeeded() }
        enableOfflineModeSideEffects()
    }

    /**
     * Reapplies the page's appearance after a trait collection
     * change.
     *
     * If the chat page is presented, the update waits until it
     * closes.
     */
    fun traitCollectionChanged() {
        if (!dependencies.isChatPagePresented()) return
        ChatPageStateService.addEffectUponIsPresented(
            state = false,
            id = ChatPageStateServiceEffectID.updateAppearance,
        ) {
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
                dependencies.resolveCurrentUser(setOf(User.DataType.CONVERSATIONS, User.DataType.MESSAGES))
                DangerZone.deleteConversationsAction.perform()
            } catch (exception: Exception) {
                Logger.log(exception, with = AlertType.toast)
            }
        }
    }

    /**
     * Keeps the open chat page consistent with a session store
     * change.
     *
     * If a message is being sent, handling waits until the send
     * completes. Otherwise, this method re-fetches the last
     * message of one-to-one conversations to pick up read receipt
     * changes, marks unread messages as read – updating the app
     * badge – and notifies observers that the current
     * conversation's metadata changed. If the chat page is not
     * presented, this method does nothing.
     */
    fun handleChatPageStoreChange() {
        if (!dependencies.isChatPagePresented()) return
        if (dependencies.isSendingMessage()) {
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

        serviceScope.launch { handleChatPageStoreChangeNow() }
    }

    /**
     * Reloads conversation data in response to a pull-to-refresh.
     *
     * Successive refreshes rotate through three reload depths: a full
     * reload, which force updates a shuffled third of the user's
     * conversations; a partial reload, which force updates only the
     * most recent conversation; and a minimal reload, which force
     * updates none. Every refresh re-resolves the current user's data
     * and periodically syncs the contact pair archive.
     *
     * @throws Exception if resolving user data or syncing contacts
     *   fails.
     */
    suspend fun reloadData() {
        try {
            reloadData(currentReloadType)
        } finally {
            sharedStates.reloadingConversationIDKeys.value = emptySet()
        }
    }

    // MARK: - Auxiliary

    /**
     * Logs how long the app's content took to load, once per page
     * appearance.
     *
     * The report includes per-conversation load time and a breakdown
     * of the user's messages by content type. On prerelease builds,
     * the report also appears as a toast.
     */
    fun showSecondsToLoadToastIfNeeded() {
        if (didShowSecondsToLoadToast) return
        didShowSecondsToLoadToast = true

        Task.debounced(TASK_ID_SHOW_SECONDS_TO_LOAD_TOAST, delay = SECONDS_TO_LOAD_TOAST_DELAY) {
            val message = secondsToLoadMessage()
            Logger.log(message, domain = LoggerDomain.conversation)

            if (Build.milestone == Build.Milestone.GENERAL_RELEASE) return@debounced
            Toast.show(
                Toast(
                    message = message,
                    perpetuation = Toast.PerpetuationStrategy.Ephemeral(SECONDS_TO_LOAD_TOAST_DURATION),
                ),
            )
        }
    }

    /**
     * Downloads a speech voice for the current user's language if
     * none is installed, so that messages in the language can be
     * spoken.
     */
    internal fun downloadVoiceIfNeeded() {
        val languageCode = dependencies.currentUser()?.languageCode ?: return
        dependencies.downloadVoiceIfNeeded(languageCode)
    }

    internal fun secondsToLoadMessage(): String {
        val currentUser = dependencies.currentUser()
        val numberOfConversations =
            currentUser
                ?.conversations
                ?.visibleForCurrentUser
                ?.size
                ?: currentUser?.conversationIDs?.size
                ?: 1

        val secondsToLoad = max(abs(Date().time - Application.loadStartDate.time) / MILLISECONDS_PER_SECOND - 1, 0L)
        val secondsPerConversation = String.format(Locale.US, "%.2f", secondsToLoad.toFloat() / numberOfConversations.toFloat())
        val suffix =
            if ((secondsPerConversation.toFloatOrNull() ?: 0f) <= SECONDS_PER_CONVERSATION_SUFFIX_THRESHOLD) {
                null
            } else {
                " (${secondsPerConversation}s/conversation)"
            }

        val allMessages =
            (currentUser?.conversations ?: sessionStore.conversations.values.toList())
                .visibleForCurrentUser
                .mapNotNull { it.messages }
                .flatten()

        val uniqueMessages = allMessages.uniquedByID
        val audioMessageCount = uniqueMessages.count { it.contentType.isAudio }
        val mediaMessageCount = uniqueMessages.count { it.contentType.isMedia }
        val totalMessageCount = uniqueMessages.size
        val textMessageCount = totalMessageCount - (audioMessageCount + mediaMessageCount)

        val safeMessageCount = if (totalMessageCount == 0) 1.0 else totalMessageCount.toDouble()
        val audioMessagePercent = (audioMessageCount / safeMessageCount).roundedString
        val mediaMessagePercent = (mediaMessageCount / safeMessageCount).roundedString
        val textMessagePercent = (textMessageCount / safeMessageCount).roundedString

        var addendum = ""
        if (totalMessageCount > 0) {
            addendum = "\nUser has $totalMessageCount messages and $numberOfConversations conversations."
            addendum += "\n$textMessagePercent% text, $audioMessagePercent% audio, $mediaMessagePercent% media."
        }

        val seconds = "second${if (secondsToLoad == 1L) "" else "s"}"
        return "Loaded content in $secondsToLoad $seconds${suffix ?: ""}.$addendum"
    }

    private fun enableOfflineModeSideEffects() {
        fun showOfflineModeToast() {
            Toast.show(
                Toast(
                    Toast.ToastType.Capsule(ToastStyle.WARNING),
                    message = LocalizedStringKey.OfflineMode.localized(),
                    perpetuation = Toast.PerpetuationStrategy.Ephemeral(OFFLINE_MODE_TOAST_DURATION),
                ),
            )
        }

        // An offline startup leaves the navigation bar appearance unset;
        // this reapplies it.
        fun updateAppearance() {
            Logger.log(
                "Intercepted offline startup navigation bar appearance bug.",
                domain = LoggerDomain.bugPrevention,
            )

            Task.delayed(by = UPDATE_APPEARANCE_DELAY) {
                sharedEvents.traitCollectionChanged.send(Unit)
            }
        }

        connectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.CHECK_FOR_UPDATES) {
            if (!Build.isOnline) return@addEffectUponConnectionChanged
            serviceScope.launch {
                try {
                    updateService.promptToUpdateIfNeeded()
                } catch (exception: Exception) {
                    Logger.log(exception, with = AlertType.toastInPrerelease)
                }
            }
        }

        connectionStatusService.addEffectUponConnectionChanged(ConnectionStatusServiceEffectID.SHOW_OFFLINE_MODE_TOAST) {
            if (Build.isOnline) return@addEffectUponConnectionChanged
            showOfflineModeToast()
        }

        if (Build.isOnline) return
        updateAppearance()
        showOfflineModeToast()
    }

    internal suspend fun handleChatPageStoreChangeNow() {
        val currentConversation = dependencies.currentConversation() ?: return

        // Re-fetch the last message in 1:1 conversations to pick up
        // read receipt changes that don't affect the conversation hash.
        if (currentConversation.participants.size == 2) {
            val lastMessageID = currentConversation.messages?.lastOrNull()?.id ?: currentConversation.messageIDs.lastOrNull()
            if (lastMessageID != null) resolveLastMessage(lastMessageID, currentConversation)
        }

        // Mark unread messages as read and update the badge.
        try {
            if (dependencies.updateReadDateForUnreadMessages()) return
        } catch (exception: Exception) {
            Logger.log(exception, domain = LoggerDomain.conversation)
            return
        }

        if (!matchesCurrentConversation(currentConversation.id.key)) return
        // The chat title follows the conversation, except while the
        // recipient bar names the new chat.
        if (!dependencies.isNewChatPagePresented()) {
            ConversationCellViewData(currentConversation)?.titleLabelText?.let { ChatPageViewService.setNavigationTitle(it) }
        }

        sharedEvents.currentConversationMetadataChanged.send(Unit)
    }

    private fun matchesCurrentConversation(idKey: String): Boolean = dependencies.currentConversation()?.id?.key == idKey

    private suspend fun reloadData(type: ReloadType) {
        val conversations =
            dependencies
                .currentUser()
                ?.conversations
                ?.visibleForCurrentUser
                ?.sortedByLatestMessageSentDate
        val firstConversation = conversations?.firstOrNull()
        if (conversations != null && firstConversation != null && (type == ReloadType.FULL || type == ReloadType.PARTIAL)) {
            var conversationsToReload = listOf(firstConversation)
            if (type == ReloadType.FULL) {
                conversationsToReload =
                    if (conversations.size > FULL_RELOAD_SHUFFLE_THRESHOLD) {
                        conversations.shuffled().take(conversations.size / FULL_RELOAD_FRACTION_DENOMINATOR + 1)
                    } else {
                        conversations
                    }
            }

            sharedStates.reloadingConversationIDKeys.value =
                conversationsToReload.map { it.id.key }.toSet()
            conversationsToReload.forEach { it.markStaleLocally() }
        }

        try {
            dependencies.resolveCurrentUser(allDataTypes)
        } finally {
            currentReloadType = currentReloadType.next
        }

        syncContactPairArchiveIfNeeded()
    }

    internal suspend fun syncContactPairArchiveIfNeeded() {
        val lastContactSyncDate = dependencies.lastContactSyncDate()
        val shouldSync =
            if (!dependencies.hasContactsBesidesCurrentUser()) {
                true
            } else if (lastContactSyncDate != null) {
                abs(Date().time - lastContactSyncDate.time) / MILLISECONDS_PER_SECOND >=
                    ConversationsPageViewFloats.CONTACT_SYNC_INTERVAL_SECONDS
            } else {
                true
            }

        if (!shouldSync) return
        try {
            dependencies.syncContactPairArchive()
            dependencies.setLastContactSyncDate(Date())
        } catch (exception: Exception) {
            if (!exception.isEqual(toAny = listOf(AppException.mismatchedHashAndCallingCode, AppException.notAuthorizedForContacts))) {
                throw exception
            }
        }
    }

    private suspend fun resolveLastMessage(
        lastMessageID: String,
        conversation: Conversation,
    ) {
        try {
            dependencies.resolveLastMessage(lastMessageID, conversation)
        } catch (exception: Exception) {
            Logger.log(exception, domain = LoggerDomain.conversation)
        }
    }

    /**
     * Evaluates several user-facing prompts in priority order: a
     * notification permission request when the permission is
     * undetermined, otherwise an invitation suggestion, otherwise a
     * review prompt.
     */
    internal suspend fun showPromptsIfNeeded() {
        try {
            if (dependencies.notificationPermissionStatus() == PermissionService.PermissionStatus.UNKNOWN) {
                dependencies.requestNotificationPermission()
            } else if (!dependencies.suggestInvitationIfNeeded()) {
                dependencies.promptToReview()
            }
        } catch (exception: Exception) {
            Logger.log(exception)
        }
    }

    // MARK: - Companion

    private companion object {
        const val FULL_RELOAD_FRACTION_DENOMINATOR = 3
        const val FULL_RELOAD_SHUFFLE_THRESHOLD = 5
        const val MILLISECONDS_PER_SECOND = 1000L
        val OFFLINE_MODE_TOAST_DURATION = 10.seconds
        val PROMPT_DELAY = 1.seconds

        @Suppress("VariableMaxLength")
        const val SECONDS_PER_CONVERSATION_SUFFIX_THRESHOLD = 0.05f

        val SECONDS_TO_LOAD_TOAST_DELAY = 1.seconds
        val SECONDS_TO_LOAD_TOAST_DURATION = 10.seconds
        const val TASK_ID_SHOW_SECONDS_TO_LOAD_TOAST = "ConversationsPageViewService/showSecondsToLoadToast"
        val UPDATE_APPEARANCE_DELAY = 500.milliseconds
    }
}
