//
//  NavigatorStates.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 06/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

/**
 * The top-level navigation state.
 *
 * [RootNavigatorState] tracks the presented full-screen [modal] – which
 * determines the top-level screen – along with nested state for each
 * child navigation flow. It defaults to [ModalPath.Splash] on launch.
 */
data class RootNavigatorState(
    val chat: ChatNavigatorState = ChatNavigatorState(),
    val onboarding: OnboardingNavigatorState = OnboardingNavigatorState(),
    val settings: SettingsNavigatorState = SettingsNavigatorState(),
    val userContent: UserContentNavigatorState = UserContentNavigatorState(),
    val modal: ModalPath? = ModalPath.Splash,
    val sheet: SheetPath? = null,
    val stack: List<SeguePath> = emptyList(),
) {
    /** The full-screen destinations at the root level. */
    sealed interface ModalPath : Paths {
        data object Onboarding : ModalPath

        data object Splash : ModalPath

        data object UserContent : ModalPath
    }

    /** The push destinations on the root stack. */
    sealed interface SeguePath : Paths

    /** The sheet destinations at the root level. */
    sealed interface SheetPath : Paths
}

/** The onboarding flow's navigation state. */
data class OnboardingNavigatorState(
    val stack: List<SeguePath> = emptyList(),
) {
    sealed interface SeguePath : Paths {
        data object AuthCode : SeguePath

        data object Permission : SeguePath

        data object SelectLanguage : SeguePath

        data object SignIn : SeguePath

        data object VerifyNumber : SeguePath
    }
}

/** The signed-in content flow's navigation state. */
data class UserContentNavigatorState(
    val stack: List<SeguePath> = emptyList(),
) {
    sealed interface SeguePath : Paths {
        /** The chat page for the conversation with the given key. */
        data class Chat(
            val conversationIDKey: String,
            val focusedMessageID: String? = null,
        ) : SeguePath

        /** The chat-info page for the conversation with the given key. */
        data class ChatInfo(
            val conversationIDKey: String,
        ) : SeguePath

        data object NewChat : SeguePath

        data object Settings : SeguePath

        data object ChangeLanguage : SeguePath
    }
}

/** The settings flow's navigation state. */
data class SettingsNavigatorState(
    val sheet: SheetPath? = null,
) {
    /** The sheet destinations presented over the settings flow. */
    sealed interface SheetPath : Paths {
        /** The invite QR code sheet. */
        data object InviteQRCode : SheetPath
    }
}

/** The chat flow's navigation state. */
data class ChatNavigatorState(
    val sheet: SheetPath? = null,
) {
    /** The sheet destinations presented over the chat flow. */
    sealed interface SheetPath : Paths {
        data object ContactSelector : SheetPath
    }
}

/**
 * A descriptor for the frontmost navigation destination (the
 * destination type's simple name). Used as the `view_id` in analytics and error-report metadata.
 *
 * The frontmost sheet wins; otherwise the top of the active modal's
 * stack, falling back to the modal itself.
 */
val RootNavigatorState.descriptor: String?
    get() {
        sheet?.let { return it.pathDescriptor }
        chat.sheet?.let { return it.pathDescriptor }
        settings.sheet?.let { return it.pathDescriptor }

        val modalPath = modal ?: return null
        val top =
            when (modalPath) {
                RootNavigatorState.ModalPath.Onboarding -> onboarding.stack.lastOrNull()
                RootNavigatorState.ModalPath.UserContent -> userContent.stack.lastOrNull()
                RootNavigatorState.ModalPath.Splash -> null
            }
        return (top ?: modalPath).pathDescriptor
    }

private val Paths.pathDescriptor: String
    get() = this::class.simpleName ?: "Unknown"
