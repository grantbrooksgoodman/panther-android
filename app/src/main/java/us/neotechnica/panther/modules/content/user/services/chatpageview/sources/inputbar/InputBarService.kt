//
//  InputBarService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.inputbar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.designsystem.modules.alertkit.extensions.sanitized
import us.neotechnica.panther.modules.common.extensions.isOnline
import us.neotechnica.panther.modules.content.user.extensions.isEmpty
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.inputbar.sources.InputBarActionHandlerService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.networking.sources.conversation.models.Conversation
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

/**
 * The service that manages the message input bar.
 *
 * Use [InputBarService] to keep the chat page's input bar in sync with
 * the conversation's state. It enables or disables the send and
 * attach-media buttons, holds the input field's text and first
 * responder status, and presents the sending state while a message is
 * being delivered.
 *
 * @param dependencies The collaborators the service reads from; the
 *   defaults resolve the live services.
 */
class InputBarService(
    private val dependencies: Dependencies = Dependencies(),
) {
    // MARK: - Types

    /**
     * The collaborators the input bar service reads from.
     *
     * @property currentConversation Returns the current conversation.
     * @property isNewChatPagePresented Returns whether the new chat
     *   page is presented.
     * @property isOnline Returns whether the device is online.
     * @property isRecipientBarFirstResponder Returns whether the
     *   recipient bar's text field is the first responder.
     * @property isSendingMessage Returns whether a message send is in
     *   flight.
     */
    data class Dependencies(
        val currentConversation: () -> Conversation? = { DependencyValues.current.clientSession.entity.conversation.currentConversation },
        val isNewChatPagePresented: () -> Boolean = {
            DependencyValues.current.navigation.state.value.userContent.stack
                .lastOrNull() == UserContentNavigatorState.SeguePaths.NewChat
        },
        val isOnline: () -> Boolean = { Build.isOnline },
        val isRecipientBarFirstResponder: () -> Boolean = { RecipientBarService.layout.isTextFieldFirstResponder.value },
        val isSendingMessage: () -> Boolean = { MessageDeliveryService.isSendingMessage.value },
    )

    // MARK: - Properties

    /** The service that handles the input bar's button actions. */
    val actionHandler = InputBarActionHandlerService()

    private val mutableIsAttachMediaButtonEnabled = MutableStateFlow(false)
    private val mutableIsFirstResponder = MutableStateFlow(false)
    private val mutableIsSendButtonEnabled = MutableStateFlow(false)
    private val mutableIsShowingSendingUI = MutableStateFlow(false)
    private val mutableShouldBecomeFirstResponder = MutableStateFlow(false)
    private val mutableText = MutableStateFlow("")

    // MARK: - Computed Properties

    /** Whether the attach-media button is enabled. */
    val isAttachMediaButtonEnabled: StateFlow<Boolean> = mutableIsAttachMediaButtonEnabled.asStateFlow()

    /** Whether the input field is the first responder. */
    val isFirstResponder: StateFlow<Boolean> = mutableIsFirstResponder.asStateFlow()

    /** Whether the send button is enabled. */
    val isSendButtonEnabled: StateFlow<Boolean> = mutableIsSendButtonEnabled.asStateFlow()

    /** Whether the input bar presents its sending state. */
    val isShowingSendingUI: StateFlow<Boolean> = mutableIsShowingSendingUI.asStateFlow()

    /**
     * Whether the input field has been asked to become the first
     * responder and has not yet taken focus.
     */
    val shouldBecomeFirstResponder: StateFlow<Boolean> = mutableShouldBecomeFirstResponder.asStateFlow()

    /** The input field's text. */
    val text: StateFlow<String> = mutableText.asStateFlow()

    /** A Boolean value that indicates whether the attach-media button should be enabled. */
    val shouldEnableAttachMediaButton: Boolean
        get() = resolveShouldEnableAttachMediaButton()

    /** A Boolean value that indicates whether the send button should be enabled. */
    val shouldEnableSendButton: Boolean
        get() = resolveShouldEnableSendButton()

    // MARK: - Configure Input Bar

    /**
     * Configures the input bar for its current state, enabling or
     * disabling the send and attach-media buttons to match.
     *
     * @param forceUpdate A Boolean value that determines whether to
     *   reconfigure the buttons even when they already match.
     */
    fun configureInputBar(forceUpdate: Boolean = false) {
        val shouldEnableAttachMediaButton = shouldEnableAttachMediaButton
        val shouldEnableSendButton = shouldEnableSendButton
        if (!forceUpdate &&
            mutableIsAttachMediaButtonEnabled.value == shouldEnableAttachMediaButton &&
            mutableIsSendButtonEnabled.value == shouldEnableSendButton
        ) {
            return
        }

        mutableIsAttachMediaButtonEnabled.value = shouldEnableAttachMediaButton
        mutableIsSendButtonEnabled.value = shouldEnableSendButton
    }

    // MARK: - Set Attach Media Button Is Enabled

    /**
     * Sets whether the attach-media button is enabled.
     *
     * @param isEnabled A Boolean value that indicates whether the
     *   attach-media button is enabled.
     */
    fun setAttachMediaButtonIsEnabled(isEnabled: Boolean) {
        mutableIsAttachMediaButtonEnabled.value = isEnabled
    }

    // MARK: - Become First Responder

    /**
     * Asks the input field to become the first responder.
     *
     * The request stands until the field takes focus, so it survives
     * the field being recreated.
     */
    fun becomeFirstResponder() {
        mutableShouldBecomeFirstResponder.value = true
    }

    // MARK: - Set Is First Responder

    /**
     * Records whether the input field is the first responder.
     *
     * @param isFirstResponder A Boolean value that indicates whether
     *   the input field has focus.
     */
    fun setIsFirstResponder(isFirstResponder: Boolean) {
        mutableIsFirstResponder.value = isFirstResponder
        if (isFirstResponder) mutableShouldBecomeFirstResponder.value = false
    }

    // MARK: - Set Send Button Is Enabled

    /**
     * Sets whether the send button is enabled.
     *
     * @param isEnabled A Boolean value that indicates whether the send
     *   button is enabled.
     */
    fun setSendButtonIsEnabled(isEnabled: Boolean) {
        mutableIsSendButtonEnabled.value = isEnabled
    }

    // MARK: - Text View Did Change

    /**
     * Updates the input field's text and reconfigures the buttons.
     *
     * @param text The input field's new text.
     */
    fun textViewDidChange(text: String) {
        mutableText.value = text
        configureInputBar()
    }

    // MARK: - Toggle Sending UI

    /**
     * Shows or hides the input bar's sending state.
     *
     * While the sending state is shown, the send button animates and
     * the input field and attach-media button are disabled.
     *
     * @param on A Boolean value that indicates whether to show the
     *   sending state.
     * @param clearInputTextViewText A Boolean value that determines
     *   whether to clear the input field's text when showing the
     *   sending state.
     */
    fun toggleSendingUI(
        on: Boolean,
        clearInputTextViewText: Boolean = true,
    ) {
        if (on) {
            if (clearInputTextViewText) mutableText.value = ""
            setAttachMediaButtonIsEnabled(false)
        } else {
            setAttachMediaButtonIsEnabled(shouldEnableAttachMediaButton)
        }

        mutableIsShowingSendingUI.value = on
        mutableIsSendButtonEnabled.value = !on && shouldEnableSendButton
    }

    // MARK: - Computed Property Getters

    private fun resolveShouldEnableAttachMediaButton(): Boolean {
        if (!dependencies.isOnline()) return false

        val isConversationEmpty = dependencies.currentConversation()?.isEmpty ?: true
        return !isConversationEmpty && !dependencies.isRecipientBarFirstResponder() && !dependencies.isSendingMessage()
    }

    private fun resolveShouldEnableSendButton(): Boolean {
        val isConversationEmpty = dependencies.currentConversation()?.isEmpty ?: true
        val isRecipientBarFirstResponder = dependencies.isRecipientBarFirstResponder()

        // Text sends are allowed offline – handled by the outbox fail-fast
        // and auto-retry path – except from the new chat page, where no
        // conversation exists yet to stage them against.
        if (!dependencies.isOnline() && dependencies.isNewChatPagePresented()) return false

        val isTextViewTextBlank = mutableText.value.sanitized.isBlank()
        return !isConversationEmpty && !isRecipientBarFirstResponder && !dependencies.isSendingMessage() && !isTextViewTextBlank
    }
}
