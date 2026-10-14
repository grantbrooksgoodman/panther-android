//
//  RootNavigator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import us.neotechnica.panther.bundle.navigators.ChatNavigatorState
import us.neotechnica.panther.bundle.navigators.OnboardingNavigatorState
import us.neotechnica.panther.bundle.navigators.SettingsNavigatorState
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.NavigatorState
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Paths

/**
 * The top-level navigation state.
 *
 * [RootNavigatorState] tracks the presented full-screen [modal] – which
 * determines the top-level screen – along with nested state for each
 * child navigation flow. It defaults to [ModalPaths.Splash] on launch.
 *
 * @property chat The chat flow's state.
 * @property onboarding The onboarding flow's state.
 * @property settings The settings flow's state.
 * @property userContent The signed-in content flow's state.
 */
data class RootNavigatorState(
    val chat: ChatNavigatorState = ChatNavigatorState(),
    val onboarding: OnboardingNavigatorState = OnboardingNavigatorState(),
    val settings: SettingsNavigatorState = SettingsNavigatorState(),
    val userContent: UserContentNavigatorState = UserContentNavigatorState(),
    override val modal: ModalPaths? = ModalPaths.Splash,
    override val sheet: SheetPaths? = null,
    override val stack: List<SeguePaths> = emptyList(),
) : NavigatorState<RootNavigatorState.ModalPaths, RootNavigatorState.SeguePaths, RootNavigatorState.SheetPaths> {
    // MARK: - Types

    /** The full-screen destinations at the root level. */
    sealed interface ModalPaths : Paths {
        /** The onboarding flow. */
        data object Onboarding : ModalPaths

        /** The splash screen, presented during app startup. */
        data object Splash : ModalPaths

        /** The signed-in user's content. */
        data object UserContent : ModalPaths
    }

    /** The push destinations on the root stack. */
    sealed interface SeguePaths : Paths

    /** The sheet destinations at the root level. */
    sealed interface SheetPaths : Paths
}

/** Applies [RootRoute]s to the root-level navigation state. */
object RootNavigator {
    /**
     * Returns the root-level state after applying the given route.
     *
     * @param to The route to apply.
     * @param on The state to apply it to.
     */
    fun navigate(
        to: RootRoute,
        on: RootNavigatorState,
    ): RootNavigatorState =
        when (to) {
            is RootRoute.Modal -> on.copy(modal = to.path)
        }
}

/** Routes handled by the root navigator. */
sealed interface RootRoute {
    /** Presents the given full-screen modal, or dismisses it when `null`. */
    data class Modal(
        val path: RootNavigatorState.ModalPaths?,
    ) : RootRoute
}

// MARK: - Descriptor

/**
 * A descriptor for the frontmost navigation destination (the
 * destination type's simple name), used as the `view_id` in analytics
 * and error-report metadata.
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
