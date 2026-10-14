//
//  NavigatorStates.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

/**
 * The top-level navigation state.
 *
 * [RootNavigatorState] tracks the presented full-screen [modal] – which
 * determines the top-level screen – along with nested state for each
 * child navigation flow. It defaults to [ModalPaths.Splash] on launch.
 */
data class RootNavigatorState(
    val chat: ChatNavigatorState = ChatNavigatorState(),
    val onboarding: OnboardingNavigatorState = OnboardingNavigatorState(),
    val settings: SettingsNavigatorState = SettingsNavigatorState(),
    val userContent: UserContentNavigatorState = UserContentNavigatorState(),
    val modal: ModalPaths? = ModalPaths.Splash,
    val sheet: SheetPaths? = null,
    val stack: List<SeguePaths> = emptyList(),
) {
    /** The full-screen destinations at the root level. */
    sealed interface ModalPaths : Paths {
        data object Onboarding : ModalPaths

        data object Splash : ModalPaths

        data object UserContent : ModalPaths
    }

    /** The push destinations on the root stack. */
    sealed interface SeguePaths : Paths

    /** The sheet destinations at the root level. */
    sealed interface SheetPaths : Paths
}

/** The onboarding flow's navigation state. */
data class OnboardingNavigatorState(
    val stack: List<SeguePaths> = emptyList(),
) {
    sealed interface SeguePaths : Paths {
        data object AuthCode : SeguePaths

        data object Permission : SeguePaths

        data object SelectLanguage : SeguePaths

        data object SignIn : SeguePaths

        data object VerifyNumber : SeguePaths
    }
}

/** The signed-in content flow's navigation state. */
data class UserContentNavigatorState(
    val stack: List<SeguePaths> = emptyList(),
) {
    sealed interface SeguePaths : Paths {
        /** The chat page for the conversation with the given key. */
        data class Chat(
            val conversationIDKey: String,
            val focusedMessageID: String? = null,
        ) : SeguePaths

        /** The chat-info page for the conversation with the given key. */
        data class ChatInfo(
            val conversationIDKey: String,
        ) : SeguePaths

        data object NewChat : SeguePaths

        data object Settings : SeguePaths

        data object ChangeLanguage : SeguePaths
    }
}

/** The settings flow's navigation state. */
data class SettingsNavigatorState(
    val sheet: SheetPaths? = null,
) {
    /** The sheet destinations presented over the settings flow. */
    sealed interface SheetPaths : Paths {
        /** The invite QR code sheet. */
        data object InviteQRCode : SheetPaths
    }
}

/** The chat flow's navigation state. */
data class ChatNavigatorState(
    val sheet: SheetPaths? = null,
) {
    /** The sheet destinations presented over the chat flow. */
    sealed interface SheetPaths : Paths {
        data object CameraPicker : SheetPaths

        data object ContactSelector : SheetPaths

        data object PhotoPicker : SheetPaths
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
                RootNavigatorState.ModalPaths.Onboarding -> onboarding.stack.lastOrNull()
                RootNavigatorState.ModalPaths.UserContent -> userContent.stack.lastOrNull()
                RootNavigatorState.ModalPaths.Splash -> null
            }
        return (top ?: modalPath).pathDescriptor
    }

private val Paths.pathDescriptor: String
    get() = this::class.simpleName ?: "Unknown"
