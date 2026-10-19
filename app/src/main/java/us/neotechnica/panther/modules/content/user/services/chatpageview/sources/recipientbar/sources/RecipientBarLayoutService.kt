//
//  RecipientBarLayoutService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import us.neotechnica.panther.bundle.isNewChatPageDoneToolbarButtonEnabled
import us.neotechnica.panther.modules.content.user.dependencies.inputBarService
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.inputbar.InputBarService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates
import us.neotechnica.panther.subsystem.modules.shared.models.SharedStates

/**
 * The service that describes the recipient bar's subviews.
 *
 * [RecipientBarLayoutService] holds the state the recipient bar
 * renders – its text field's text and focus, the select-contact
 * button's enablement, the message input bar's placeholder, and
 * whether the bar accepts interaction – and relays first-responder
 * requests to the views that host the text field and the input bar.
 */
class RecipientBarLayoutService {
    // MARK: - Types

    /** A view that can be asked to become the first responder. */
    enum class FirstResponder {
        /** The message input bar. */
        INPUT_BAR,

        /** The recipient bar's text field. */
        TEXT_FIELD,
    }

    // MARK: - Dependencies

    private val inputBarService: InputBarService by Dependency { it.inputBarService }
    private val sharedStates: SharedStates by Dependency { it.sharedStates }

    // MARK: - Properties

    private val mutableFirstResponderRequests = MutableSharedFlow<FirstResponder>(extraBufferCapacity = 1)
    private val mutableInputBarPlaceholder = MutableStateFlow<String?>("")
    private val mutableIsSelectContactButtonEnabled = MutableStateFlow(true)
    private val mutableIsTextFieldFirstResponder = MutableStateFlow(false)
    private val mutableIsUserInteractionEnabled = MutableStateFlow(true)
    private val mutableTextFieldText = MutableStateFlow("")

    private var wasTextFieldFirstResponder = false

    // MARK: - Computed Properties

    /** Requests for a view to become the first responder. */
    val firstResponderRequests: SharedFlow<FirstResponder> = mutableFirstResponderRequests.asSharedFlow()

    /** The message input bar's placeholder, or `null` for none. */
    val inputBarPlaceholder: StateFlow<String?> = mutableInputBarPlaceholder.asStateFlow()

    /**
     * A Boolean value that indicates whether the select-contact
     * button is enabled.
     */
    val isSelectContactButtonEnabled: StateFlow<Boolean> = mutableIsSelectContactButtonEnabled.asStateFlow()

    /**
     * A Boolean value that indicates whether the text field is
     * the first responder.
     */
    val isTextFieldFirstResponder: StateFlow<Boolean> = mutableIsTextFieldFirstResponder.asStateFlow()

    /**
     * A Boolean value that indicates whether the recipient bar
     * accepts interaction.
     */
    val isUserInteractionEnabled: StateFlow<Boolean> = mutableIsUserInteractionEnabled.asStateFlow()

    /** The text field's text. */
    val textFieldText: StateFlow<String> = mutableTextFieldText.asStateFlow()

    // MARK: - First Responders

    /** Asks the message input bar to become the first responder. */
    fun inputBarBecomeFirstResponder() {
        mutableFirstResponderRequests.tryEmit(FirstResponder.INPUT_BAR)
    }

    /**
     * Asks the recipient bar's text field to become the first
     * responder.
     */
    fun textFieldBecomeFirstResponder() {
        mutableFirstResponderRequests.tryEmit(FirstResponder.TEXT_FIELD)
    }

    // MARK: - Remove from Superview

    /**
     * Releases the recipient bar once the conversation exists,
     * re-enabling the new chat page's done button.
     */
    fun removeFromSuperview() {
        sharedStates.isNewChatPageDoneToolbarButtonEnabled.value = true
    }

    // MARK: - Setters

    /**
     * Records the message input bar's placeholder.
     *
     * @param placeholder The placeholder to display, or `null`
     *   for none.
     */
    fun setInputBarPlaceholder(placeholder: String?) {
        mutableInputBarPlaceholder.value = placeholder
    }

    /**
     * Enables or disables the select-contact button.
     *
     * @param isEnabled A Boolean value that indicates whether the
     *   button responds to taps.
     */
    fun setIsSelectContactButtonEnabled(isEnabled: Boolean) {
        mutableIsSelectContactButtonEnabled.value = isEnabled
    }

    /**
     * Records whether the text field is the first responder.
     *
     * @param isFirstResponder A Boolean value that indicates
     *   whether the text field has focus.
     */
    fun setIsTextFieldFirstResponder(isFirstResponder: Boolean) {
        mutableIsTextFieldFirstResponder.value = isFirstResponder
        // The input bar's buttons read this status, so they follow it.
        inputBarService.configureInputBar()
    }

    /**
     * Enables or disables user interaction with the recipient bar.
     *
     * When disabling, the method remembers whether the text field was
     * the first responder and restores that status when re-enabling.
     *
     * @param isUserInteractionEnabled A Boolean value that indicates
     *   whether user interaction is enabled.
     */
    fun setIsUserInteractionEnabled(isUserInteractionEnabled: Boolean) {
        sharedStates.isNewChatPageDoneToolbarButtonEnabled.value = isUserInteractionEnabled

        when (isUserInteractionEnabled) {
            true -> {
                mutableIsUserInteractionEnabled.value = true
                if (wasTextFieldFirstResponder) textFieldBecomeFirstResponder()
            }

            false -> {
                wasTextFieldFirstResponder = mutableIsTextFieldFirstResponder.value
                mutableIsUserInteractionEnabled.value = false
            }
        }
    }

    /**
     * Records the text field's text.
     *
     * @param text The text the field displays.
     */
    fun setTextFieldText(text: String) {
        mutableTextFieldText.value = text
    }

    // MARK: - Reset

    /** Restores the layout state for the next composition. */
    fun reset() {
        mutableInputBarPlaceholder.value = ""
        mutableIsSelectContactButtonEnabled.value = true
        mutableIsTextFieldFirstResponder.value = false
        mutableIsUserInteractionEnabled.value = true
        mutableTextFieldText.value = ""
        wasTextFieldFirstResponder = false
    }
}
