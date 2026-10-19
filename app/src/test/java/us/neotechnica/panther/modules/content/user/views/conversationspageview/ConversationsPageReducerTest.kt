//
//  ConversationsPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.conversationspageview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.bundle.conversationsSearchQuery
import us.neotechnica.panther.bundle.navigators.UserContentNavigatorState
import us.neotechnica.panther.modules.content.user.constants.ConversationsPageViewFloats
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.navigation.RootNavigationService.Route
import us.neotechnica.panther.navigation.RootNavigatorState
import us.neotechnica.panther.navigation.RootRoute
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.shared.extensions.sharedStates

/**
 * Exercises the conversations page reducer: the searching semantics,
 * the modal gate on session store changes, and the compose button
 * pulse actions.
 */
class ConversationsPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = ConversationsPageReducer()

    @Before
    fun setUp() {
        environment.install("conversations-page-reducer-test")
    }

    // MARK: - Searching

    @Test
    fun `searching reflects the search field focus and blocks pull-to-refresh`() {
        val searching = reducer.reduce(ConversationsPageReducer.State(), ConversationsPageReducer.Action.IsSearchingChanged(true)).state
        assertTrue(searching.isSearching)

        val refreshed = reducer.reduce(searching, ConversationsPageReducer.Action.PulledToRefresh).state
        assertFalse(refreshed.isRefreshing)

        val stopped = reducer.reduce(searching, ConversationsPageReducer.Action.IsSearchingChanged(false)).state
        assertFalse(stopped.isSearching)
    }

    @Test
    fun `a changed search query is stored and shared, and an unchanged one is ignored`() {
        val result = reducer.reduce(ConversationsPageReducer.State(), ConversationsPageReducer.Action.SearchQueryChanged("ada"))

        assertEquals("ada", result.state.searchQuery)
        assertEquals("ada", DependencyValues.current.sharedStates.conversationsSearchQuery.value)

        val unchanged = reducer.reduce(result.state, ConversationsPageReducer.Action.SearchQueryChanged("ada"))
        assertEquals(result.state, unchanged.state)
    }

    // MARK: - Session Store Changes

    @Test
    fun `a store change refreshes the list only while the user content modal is presented`() {
        val navigation = DependencyValues.current.navigation
        val state = ConversationsPageReducer.State()

        navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)))
        val covered = reducer.reduce(state, ConversationsPageReducer.Action.SessionStoreDidChange).state
        assertEquals(state.conversationsChangeToken, covered.conversationsChangeToken)

        navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.UserContent)))
        val refreshed = reducer.reduce(state, ConversationsPageReducer.Action.SessionStoreDidChange).state
        assertNotEquals(state.conversationsChangeToken, refreshed.conversationsChangeToken)
        navigation.navigate(Route.Root(RootRoute.Modal(RootNavigatorState.ModalPaths.Splash)))
    }

    // MARK: - Compose Button Pulse

    @Test
    fun `the animation amount set action stores the amount`() {
        val amount = ConversationsPageViewFloats.COMPOSE_TOOLBAR_BUTTON_ANIMATION_AMOUNT

        val result =
            reducer.reduce(
                ConversationsPageReducer.State(),
                ConversationsPageReducer.Action.ComposeToolbarButtonAnimationAmountSet(amount),
            )

        assertEquals(amount, result.state.animationAmount)
    }

    @Test
    fun `the trait collection change regenerates the compose button identity`() {
        val state = ConversationsPageReducer.State()

        val result = reducer.reduce(state, ConversationsPageReducer.Action.TraitCollectionChanged)

        assertNotEquals(state.composeToolbarButtonViewID, result.state.composeToolbarButtonViewID)
    }

    @Test
    fun `the second appearance refreshes the list while the first only records it`() {
        val state = ConversationsPageReducer.State()

        val first = reducer.reduce(state, ConversationsPageReducer.Action.ViewAppeared).state
        assertTrue(first.didAppear)
        assertEquals(state.conversationsChangeToken, first.conversationsChangeToken)

        val second = reducer.reduce(first, ConversationsPageReducer.Action.ViewAppeared).state
        assertNotEquals(first.conversationsChangeToken, second.conversationsChangeToken)
    }

    @Test
    fun `only a pushed chat covers the list for its lifecycle`() {
        assertTrue(isCoveredByPush(UserContentNavigatorState.SeguePaths.Chat("c1")))
        assertFalse(isCoveredByPush(UserContentNavigatorState.SeguePaths.Settings))
        assertFalse(isCoveredByPush(UserContentNavigatorState.SeguePaths.NewChat))
        assertFalse(isCoveredByPush(null))
    }
}
