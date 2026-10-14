//
//  InputBarConfigService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.inputbar.sources

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import us.neotechnica.panther.R
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.designsystem.modules.componentkit.models.MessageInputBarImages
import us.neotechnica.panther.designsystem.modules.theming.services.ThemeService
import us.neotechnica.panther.modules.common.extensions.isOnline
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceStrings
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
 * the appropriate images for the send and attach-media buttons in the
 * current state.
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

    /**
     * Returns the input bar's attach and send button images for the
     * current theme and appearance.
     *
     * @return The input bar's button images.
     */
    @Composable
    fun images(): MessageInputBarImages {
        val isDarkModeActive = ThemeService.isDarkModeActive(isSystemInDarkTheme())
        return MessageInputBarImages(
            attachMediaButton = painterResource(attachMediaButtonImageResource(isHighlighted = false, isDarkModeActive)),
            attachMediaButtonHighlighted = painterResource(attachMediaButtonImageResource(isHighlighted = true, isDarkModeActive)),
            sendButton = painterResource(sendButtonImageResource(isHighlighted = false)),
            sendButtonHighlighted = painterResource(sendButtonImageResource(isHighlighted = true)),
        )
    }

    /**
     * Returns the drawable resource of the attach-media button's image:
     * a plus over a translucent disc, in the variant for the current
     * appearance.
     *
     * @param isHighlighted A Boolean value that indicates whether to
     *   return the image the button shows while pressed.
     * @param isDarkModeActive A Boolean value that indicates whether dark
     *   mode is active.
     *
     * @return The drawable resource identifier.
     */
    @DrawableRes
    fun attachMediaButtonImageResource(
        isHighlighted: Boolean,
        isDarkModeActive: Boolean,
    ): Int =
        if (isDarkModeActive) {
            if (isHighlighted) R.drawable.plus_dark_highlighted else R.drawable.plus_dark
        } else {
            if (isHighlighted) R.drawable.plus_light_highlighted else R.drawable.plus_light
        }

    /**
     * Returns the drawable resource of the send button's image.
     *
     * The app's default theme uses the blue send image; other themes
     * use the alternate image.
     *
     * @param isHighlighted A Boolean value that indicates whether to
     *   return the image the button shows while pressed.
     *
     * @return The drawable resource identifier.
     */
    @DrawableRes
    fun sendButtonImageResource(isHighlighted: Boolean): Int =
        if (ThemeService.isAppDefaultThemeApplied) {
            if (isHighlighted) R.drawable.send_highlighted else R.drawable.send
        } else {
            if (isHighlighted) R.drawable.send_alternate_highlighted else R.drawable.send_alternate
        }
}
