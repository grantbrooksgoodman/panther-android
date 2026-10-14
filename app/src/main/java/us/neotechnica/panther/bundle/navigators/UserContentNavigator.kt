//
//  UserContentNavigator.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.bundle.navigators

import us.neotechnica.panther.subsystem.modules.navigation.interfaces.NavigatorState
import us.neotechnica.panther.subsystem.modules.navigation.interfaces.Paths

/** The signed-in content flow's navigation state. */
data class UserContentNavigatorState(
    override val modal: ModalPaths? = null,
    override val sheet: SheetPaths? = null,
    override val stack: List<SeguePaths> = emptyList(),
) : NavigatorState<UserContentNavigatorState.ModalPaths, UserContentNavigatorState.SeguePaths, UserContentNavigatorState.SheetPaths> {
    // MARK: - Types

    /** The full-screen destinations in the signed-in content flow; it presents none. */
    sealed interface ModalPaths : Paths

    /** The push destinations on the signed-in content stack. */
    sealed interface SeguePaths : Paths {
        /**
         * The chat page for the conversation with the given key,
         * revealing the message with the given identifier if one is
         * given.
         */
        data class Chat(
            val conversationIDKey: String,
            val focusedMessageID: String? = null,
        ) : SeguePaths

        /** The chat-info page for the conversation with the given key. */
        data class ChatInfo(
            val conversationIDKey: String,
        ) : SeguePaths

        /** The new chat page. */
        data object NewChat : SeguePaths

        /** The settings page. */
        data object Settings : SeguePaths

        /** The change language page. */
        data object ChangeLanguage : SeguePaths
    }

    /** The sheet destinations in the signed-in content flow; it presents none. */
    sealed interface SheetPaths : Paths
}

/** Applies [UserContentRoute]s to the signed-in content flow's navigation state. */
object UserContentNavigator {
    /**
     * Returns the signed-in content flow's state after applying the
     * given route.
     *
     * @param to The route to apply.
     * @param on The state to apply it to.
     */
    fun navigate(
        to: UserContentRoute,
        on: UserContentNavigatorState,
    ): UserContentNavigatorState =
        when (to) {
            UserContentRoute.Pop -> on.copy(stack = on.stack.dropLast(1))
            is UserContentRoute.Push -> on.copy(stack = on.stack + to.path)
            is UserContentRoute.Stack -> on.copy(stack = to.paths)
        }
}

/** Routes handled by the signed-in content navigator. */
sealed interface UserContentRoute {
    /** Pops the top page of the signed-in content stack. */
    data object Pop : UserContentRoute

    /** Pushes the given page onto the signed-in content stack. */
    data class Push(
        val path: UserContentNavigatorState.SeguePaths,
    ) : UserContentRoute

    /** Replaces the signed-in content stack with the given pages. */
    data class Stack(
        val paths: List<UserContentNavigatorState.SeguePaths>,
    ) : UserContentRoute
}
