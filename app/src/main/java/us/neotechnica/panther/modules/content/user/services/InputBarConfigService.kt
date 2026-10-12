//
//  InputBarConfigService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import us.neotechnica.panther.modules.common.extensions.isOnline
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
import us.neotechnica.panther.navigation.UserContentNavigatorState
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.services.Build

// MARK: - Constants Accessors

private typealias InputBarStrings = ChatPageViewServiceStrings.InputBar

/**
 * The service that supplies the input bar's configuration values and
 * button images.
 *
 * Use [InputBarConfigService] to determine whether the send button can
 * act as a record button for the current conversation, and to obtain
 * the appropriate symbol names for the send and attach-media buttons
 * in the current state.
 */
object InputBarConfigService {
    // MARK: - Computed Properties

    /**
     * A Boolean value that indicates whether the send button can be
     * configured as a record button for the current conversation.
     *
     * Audio message recording is not offered, so this is always
     * `false`.
     */
    val canShowRecordButton: Boolean
        get() = false

    // MARK: - Methods

    /**
     * Returns the attach-media button's symbol name.
     *
     * @return The symbol name.
     */
    fun attachMediaButtonImage(): String = InputBarStrings.ATTACH_MEDIA_BUTTON_IMAGE_SYSTEM_NAME

    /**
     * Returns the send button's symbol name for the given
     * configuration.
     *
     * The image shows a distinct glyph when the app is offline in a new
     * conversation.
     *
     * @param forRecording A Boolean value that indicates whether to
     *   return the record button's image rather than the send button's.
     *
     * @return The symbol name.
     */
    fun sendButtonImage(forRecording: Boolean = false): String {
        val isNewChatPagePresented =
            DependencyValues.current.navigation.state.value.userContent.stack
                .lastOrNull() == UserContentNavigatorState.SeguePaths.NewChat
        if (!Build.isOnline && forRecording && isNewChatPagePresented) {
            return InputBarStrings.SEND_BUTTON_OFFLINE_IMAGE_SYSTEM_NAME
        }

        return InputBarStrings.SEND_BUTTON_IMAGE_SYSTEM_NAME
    }
}
