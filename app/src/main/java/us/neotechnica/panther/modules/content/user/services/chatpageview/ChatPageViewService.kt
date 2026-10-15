//
//  ChatPageViewService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.bundle.LocalizedStringKey
import us.neotechnica.panther.designsystem.modules.alertkit.models.Action
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionSheet
import us.neotechnica.panther.designsystem.modules.alertkit.models.ActionStyle
import us.neotechnica.panther.modules.content.user.dependencies.chatInfoPageViewService
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.AudioMessagePlaybackService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.MediaActionHandlerService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.MediaMessagePreviewService
import us.neotechnica.panther.modules.content.user.views.chatpageview.ChatPageView
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.modules.session.state.services.retry
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.Task
import us.neotechnica.panther.subsystem.modules.localization.models.localized
import kotlin.time.Duration.Companion.milliseconds

/**
 * The service that orchestrates the chat page.
 *
 * [ChatPageViewService] holds the page's presentation configuration,
 * loads older messages into the message list, presents the failed
 * message action sheet, and exposes the navigation title the page
 * resolves.
 *
 * The chat page presents in one of three configurations:
 *
 * - The default configuration displays an existing conversation.
 * - The new chat configuration previews the conversation beneath the
 *   recipient bar.
 * - The preview configuration displays a read-only preview with the
 *   input bar hidden.
 *
 * **Important:** Lifecycle events that occur while a media preview or
 * picker is presented over the page reflect the page being covered and
 * uncovered, not presented and dismissed; consult
 * [shouldRespondToViewLifecycleEvent] before acting on them.
 */
object ChatPageViewService {
    // MARK: - Dependencies

    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }
    private val outbox: MessageOutboxService by Dependency { it.clientSession.outbox }

    // MARK: - Properties

    @Suppress("VariableMaxLength")
    private val mutableDidLoadMoreMessagesFromScrollToTop = MutableStateFlow(false)
    private val mutableNavigationTitle = MutableStateFlow<String?>(null)

    /** The configuration the page is presented in. */
    var configuration: ChatPageView.Configuration = ChatPageView.Configuration.Default()
        private set

    // MARK: - Computed Properties

    /** The service that plays audio messages. */
    val audioMessagePlayback: AudioMessagePlaybackService
        get() = AudioMessagePlaybackService

    /**
     * Whether the most recent load of older messages was triggered by
     * scrolling to the top of the list. The page scrolls to the topmost
     * message when this becomes `true`, then clears it.
     */
    val didLoadMoreMessagesFromScrollToTop: StateFlow<Boolean> = mutableDidLoadMoreMessagesFromScrollToTop.asStateFlow()

    /** The navigation title the page resolved, or `null` before it resolves. */
    val navigationTitle: StateFlow<String?> = mutableNavigationTitle.asStateFlow()

    /**
     * A Boolean value that indicates whether a view lifecycle event
     * reflects the page being presented or dismissed, rather than
     * covered by a media preview or picker.
     */
    val shouldRespondToViewLifecycleEvent: Boolean
        get() =
            !DependencyValues.current.chatInfoPageViewService.isPreviewingMedia &&
                !MediaActionHandlerService.isPresentingPickerController &&
                !MediaMessagePreviewService.isPreviewingMedia

    // MARK: - Set Configuration

    /**
     * Records the configuration the page is presented in.
     *
     * @param configuration The configuration to present the page in.
     */
    fun setConfiguration(configuration: ChatPageView.Configuration) {
        this.configuration = configuration
    }

    // MARK: - Load More Messages

    /**
     * Loads older messages into the message list.
     *
     * This method has no effect while a message send is in flight, or
     * when no older messages remain.
     *
     * @param fromScrollToTop Whether the load was triggered by
     *   scrolling to the top of the list, in which case the caller
     *   scrolls to the topmost message once the list reloads.
     *
     * @return `true` if older messages were loaded; otherwise, `false`.
     */
    fun loadMoreMessages(fromScrollToTop: Boolean): Boolean {
        if (MessageDeliveryService.isSendingMessage.value) return false

        val previousMessageCount = conversationSession.displayedMessages.value.size
        conversationSession.incrementMessageOffset()
        val didLoad = previousMessageCount != conversationSession.displayedMessages.value.size
        if (didLoad) mutableDidLoadMoreMessagesFromScrollToTop.value = fromScrollToTop
        return didLoad
    }

    // MARK: - Present Failed Message Action Sheet

    /**
     * Presents an action sheet for a failed outbox message, offering to
     * delete it or try sending it again.
     *
     * @param forMessageID The identifier of the failed outbox message.
     */
    suspend fun presentFailedMessageActionSheet(forMessageID: String) {
        ActionSheet(
            actions =
                listOf(
                    Action(
                        title = LocalizedStringKey.Delete.localized(),
                        style = ActionStyle.DESTRUCTIVE,
                    ) { outbox.remove(forMessageID) },
                    Action(
                        title = LocalizedStringKey.TryAgain.localized(),
                    ) { Task.delayed(by = 0.milliseconds) { outbox.retry(forMessageID) } },
                ),
            cancelButtonTitle = LocalizedStringKey.Cancel.localized(),
        ).present(translating = emptyList())
    }

    // MARK: - Set Navigation Title

    /**
     * Sets the navigation title of the page.
     *
     * @param navigationTitle The title to display.
     */
    fun setNavigationTitle(navigationTitle: String) {
        mutableNavigationTitle.value = navigationTitle
    }

    /** Clears the navigation title when the page is dismissed. */
    fun clearNavigationTitle() {
        mutableNavigationTitle.value = null
    }

    // MARK: - Did Scroll To Top After Loading

    /** Acknowledges the scroll to the topmost message after a scroll-to-top load. */
    fun didScrollToTopAfterLoading() {
        mutableDidLoadMoreMessagesFromScrollToTop.value = false
    }
}
