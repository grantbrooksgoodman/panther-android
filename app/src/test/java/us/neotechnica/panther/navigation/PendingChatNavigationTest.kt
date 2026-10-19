//
//  PendingChatNavigationTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.navigation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.modules.common.services.sources.NotificationService
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.sources.support.SessionTestEnvironment
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues

/**
 * Exercises the notification-tap deep link: a signed-in activity
 * showing the user content opens the chat at once, anything else
 * holds the key for the splash flow.
 */
class PendingChatNavigationTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val navigation get() = DependencyValues.current.navigation

    @Before
    fun setUp() {
        environment.install("pending-chat-navigation-test")
        PendingChatNavigation.consume()
    }

    @After
    fun tearDown() {
        PendingChatNavigation.consume()
        navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)))
    }

    // MARK: - Tests

    @Test
    fun `a tap on a signed-in warm activity opens the chat immediately`() {
        environment.signIn(SessionTestEnvironment.user(id = "me"))
        navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.UserContent)))

        NotificationService.respondToNotificationTap("-C")

        assertNull(PendingChatNavigation.consume())
        assertEquals(
            listOf(UserContentNavigatorState.SeguePaths.Chat("-C")),
            navigation.state.value.userContent.stack,
        )
    }

    @Test
    fun `a tap before sign-in is held for the splash flow`() {
        NotificationService.respondToNotificationTap("-C")

        assertEquals("-C", PendingChatNavigation.consume())
        assertEquals(emptyList<UserContentNavigatorState.SeguePaths>(), navigation.state.value.userContent.stack)
    }
}
