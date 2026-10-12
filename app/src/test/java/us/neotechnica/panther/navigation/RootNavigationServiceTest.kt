//
//  RootNavigationServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises the navigation service's route dispatch: the root modal,
 * the onboarding and user content stacks, and the chat and settings
 * sheets.
 */
class RootNavigationServiceTest {
    // MARK: - Setup

    private val service = RootNavigationService

    // MARK: - Root

    @Test
    fun `a root modal route replaces the presented modal`() {
        val state = service.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.UserContent)), on = RootNavigatorState())

        assertEquals(RootNavigatorState.ModalPaths.UserContent, state.modal)
    }

    // MARK: - Stacks

    @Test
    fun `user content pushes append, pops drop the last, and stack replaces`() {
        val chat = UserContentNavigatorState.SeguePaths.Chat("c1")
        var state = service.navigate(Route.UserContent(UserContentRoute.Push(chat)), on = RootNavigatorState())
        assertEquals(listOf(chat), state.userContent.stack)

        state = service.navigate(Route.UserContent(UserContentRoute.Push(UserContentNavigatorState.SeguePaths.Settings)), on = state)
        assertEquals(2, state.userContent.stack.size)

        state = service.navigate(Route.UserContent(UserContentRoute.Pop), on = state)
        assertEquals(listOf(chat), state.userContent.stack)

        state = service.navigate(Route.UserContent(UserContentRoute.Stack(emptyList())), on = state)
        assertEquals(emptyList<UserContentNavigatorState.SeguePaths>(), state.userContent.stack)
    }

    @Test
    fun `an onboarding pop on an empty stack is harmless`() {
        val state = service.navigate(Route.Onboarding(OnboardingRoute.Pop), on = RootNavigatorState())

        assertEquals(emptyList<OnboardingNavigatorState.SeguePaths>(), state.onboarding.stack)
    }

    // MARK: - Sheets

    @Test
    fun `chat and settings sheet routes present and dismiss`() {
        var state =
            service.navigate(
                Route.Chat(ChatRoute.Sheet(ChatNavigatorState.SheetPaths.ContactSelector)),
                on = RootNavigatorState(),
            )
        assertEquals(ChatNavigatorState.SheetPaths.ContactSelector, state.chat.sheet)

        state = service.navigate(Route.Settings(SettingsRoute.Sheet(SettingsNavigatorState.SheetPaths.InviteQRCode)), on = state)
        assertEquals(SettingsNavigatorState.SheetPaths.InviteQRCode, state.settings.sheet)

        state = service.navigate(Route.Chat(ChatRoute.Sheet(null)), on = state)
        assertNull(state.chat.sheet)
    }

    // MARK: - Coordinator

    @Test
    fun `the coordinator publishes each state and reports it`() {
        var reported: RootNavigatorState? = null
        val coordinator = NavigationCoordinator(RootNavigatorState(), RootNavigationService) { reported = it }

        coordinator.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Onboarding)))

        assertEquals(RootNavigatorState.ModalPaths.Onboarding, coordinator.state.value.modal)
        assertEquals(coordinator.state.value, reported)
    }
}
