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

    sealed interface ModalPaths : Paths

    /**
     * The push destinations on the signed-in content stack. The pages
     * iOS presents as sheets push here through the navigation stack
     * view.
     */
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

    sealed interface SheetPaths : Paths
}

/** Applies [UserContentRoute]s to the signed-in content flow's navigation state. */
object UserContentNavigator {
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
    data object Pop : UserContentRoute

    data class Push(
        val path: UserContentNavigatorState.SeguePaths,
    ) : UserContentRoute

    data class Stack(
        val paths: List<UserContentNavigatorState.SeguePaths>,
    ) : UserContentRoute
}
