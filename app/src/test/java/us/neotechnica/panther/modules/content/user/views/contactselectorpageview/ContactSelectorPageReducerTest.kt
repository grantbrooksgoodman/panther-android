//
//  ContactSelectorPageReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.designsystem.modules.foundation.views.ViewState
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.modules.networking.support.MessageDecodeEnvironment
import us.neotechnica.panther.modules.networking.support.SessionTestEnvironment
import us.neotechnica.panther.modules.session.state.services.SessionStore
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.localization.models.localized

/**
 * Exercises the contact selector reducer: the per-entry-point title,
 * placeholder, and no-results text, immediate loading from the new chat
 * page, the found-user result, and clearing it as the query changes.
 */
class ContactSelectorPageReducerTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val reducer = ContactSelectorPageReducer()

    @Before
    fun setUp() {
        environment.install("contact-selector-reducer-test")
        CommonPropertyLists.initializeForTesting(callingCodes = mapOf("US" to "1"), lookupTables = mapOf("10" to listOf("1")))
        RegionDetailService.clearCache()
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
    }

    // MARK: - Entry Points

    @Test
    fun `the new chat entry point loads immediately with the localized contacts title`() {
        val result =
            reducer.reduce(
                ContactSelectorPageReducer.State(ContactSelectorPageView.EntryPoint.NEW_CHAT_PAGE_VIEW),
                ContactSelectorPageReducer.Action.ViewAppeared,
            )

        assertEquals(ViewState.Loaded, result.state.viewState)
        assertEquals(LocalizedStringKey.Contacts.localized(), result.state.navigationTitle)
        assertEquals(LocalizedStringKey.Search.localized(), result.state.searchBarPlaceholderText)
        assertTrue(result.state.shouldShowInviteButton)
    }

    @Test
    fun `the chat info entry point stays loading while its strings resolve`() {
        val result =
            reducer.reduce(
                ContactSelectorPageReducer.State(ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW),
                ContactSelectorPageReducer.Action.ViewAppeared,
            )

        assertEquals(ViewState.Loading, result.state.viewState)
        assertEquals("Add to Conversation", result.state.navigationTitle)
        assertEquals("Search contacts or enter phone number", result.state.searchBarPlaceholderText)
    }

    @Test
    fun `a digit query from the chat info page offers the find-user prompt`() {
        val state = ContactSelectorPageReducer.State(ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW, searchQuery = "5558885555")

        assertEquals("No contacts found.\nTap to search for users with this phone number.", state.noResultsLabelText)
        assertEquals(
            LocalizedStringKey.NoResults.localized(),
            state.copy(entryPoint = ContactSelectorPageView.EntryPoint.NEW_CHAT_PAGE_VIEW).noResultsLabelText,
        )
    }

    // MARK: - Found User

    @Test
    fun `a found user becomes the sole result until the query stops matching it`() {
        val user = SessionTestEnvironment.user(id = "found", nationalNumber = "5558885555")
        SessionStore.upsertUser(user)
        val state = ContactSelectorPageReducer.State(ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW, searchQuery = "5558885555")

        val found = reducer.reduce(state, ContactSelectorPageReducer.Action.FindUserReturned(user))
        assertNotNull(found.state.foundContactPair)
        assertEquals(1, found.state.queriedContactPairs.size)

        val stillMatching = reducer.reduce(found.state, ContactSelectorPageReducer.Action.SearchQueryChanged("15558885555"))
        assertNotNull(stillMatching.state.foundContactPair)

        val cleared = reducer.reduce(found.state, ContactSelectorPageReducer.Action.SearchQueryChanged("555"))
        assertNull(cleared.state.foundContactPair)
    }

    @Test
    fun `a non lookup failure leaves the state untouched`() {
        val state = ContactSelectorPageReducer.State(ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW, searchQuery = "5558885555")

        val result =
            reducer.reduce(
                state,
                ContactSelectorPageReducer.Action.FindUserFailed(Exception("Offline.", metadata = ExceptionMetadata(this))),
            )

        assertEquals(state, result.state)
    }

    @Test
    fun `resolution failure still loads the page`() {
        val result =
            reducer.reduce(
                ContactSelectorPageReducer.State(ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW),
                ContactSelectorPageReducer.Action.ResolveFailed(Exception("Offline.", metadata = ExceptionMetadata(this))),
            )

        assertEquals(ViewState.Loaded, result.state.viewState)
    }
}
