//
//  InviteLanguagePickerReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.invitelanguagepickerview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage

/**
 * Exercises the invite language picker reducer: the localized language
 * names, search filtering, selection gating of the done button, and
 * the reset on appearance.
 */
class InviteLanguagePickerReducerTest {
    // MARK: - Setup

    private val reducer = InviteLanguagePickerReducer()

    @Before
    fun setUp() {
        RuntimeStorage.languageCodeDictionary = mapOf("en" to "English", "fr" to "French", "es" to "Spanish")
        CoreUtilities.setLanguageCode("en")
    }

    @After
    fun tearDown() {
        RuntimeStorage.remove(StoredItemKey.languageCode)
        RuntimeStorage.languageCodeDictionary = null
    }

    // MARK: - Language Names

    @Test
    fun `language names come from the localized language code dictionary`() {
        assertEquals(
            CoreUtilities.localizedLanguageCodeDictionary("en"),
            InviteLanguagePickerReducer.State().localizedLanguageNames,
        )
    }

    @Test
    fun `language names fall back to the stored dictionary when no localized dictionary exists`() {
        RuntimeStorage.languageCodeDictionary = null

        assertTrue(InviteLanguagePickerReducer.State().localizedLanguageNames.isEmpty())
    }

    @Test
    fun `queried names filter by a trimmed case insensitive search term`() {
        val state = InviteLanguagePickerReducer.State(searchQuery = " FREN ")

        assertEquals(setOf("fr"), state.queriedLanguageNames.keys)
        assertTrue(InviteLanguagePickerReducer.State(searchQuery = "zzz").queriedLanguageNames.isEmpty())
    }

    @Test
    fun `the navigation title is capitalized in English`() {
        assertEquals("Select Language", InviteLanguagePickerReducer.State().navigationTitle)
    }

    // MARK: - Selection

    @Test
    fun `selecting a language enables the done button`() {
        val result =
            reducer.reduce(
                InviteLanguagePickerReducer.State(),
                InviteLanguagePickerReducer.Action.SelectedLanguageCodeChanged("fr"),
            )

        assertEquals("fr", result.state.selectedLanguageCode)
        assertTrue(result.state.isDoneHeaderItemEnabled)
    }

    @Test
    fun `appearing resets the search query and selection`() {
        val result =
            reducer.reduce(
                InviteLanguagePickerReducer.State(isDoneHeaderItemEnabled = true, searchQuery = "fr", selectedLanguageCode = "fr"),
                InviteLanguagePickerReducer.Action.ViewAppeared,
            )

        assertFalse(result.state.isDoneHeaderItemEnabled)
        assertEquals("", result.state.searchQuery)
        assertEquals("", result.state.selectedLanguageCode)
    }

    @Test
    fun `done is ignored while nothing is selected`() {
        val state = InviteLanguagePickerReducer.State()

        val result = reducer.reduce(state, InviteLanguagePickerReducer.Action.DoneHeaderItemTapped)

        assertEquals(state, result.state)
    }
}
