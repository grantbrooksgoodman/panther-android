//
//  RegionMenuReducerTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.shared.components.regionmenu

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.services.CommonPropertyLists
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File

/**
 * Exercises the region menu reducer's query surface: search-term
 * filtering, the name-first selected title, selection by title, and
 * the capitalized English header.
 */
class RegionMenuReducerTest {
    // MARK: - Setup

    private val reducer = RegionMenuReducer()

    @Before
    fun setUp() {
        CommonPropertyLists.initializeForTesting(
            callingCodes =
                mapOf(
                    "CA" to "1",
                    "DE" to "49",
                    "FR" to "33",
                    "US" to "1",
                ),
            lookupTables = emptyMap(),
        )

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }

        CoreUtilities.setLanguageCode("en")
        RegionDetailService.clearCache()
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        RuntimeStorage.remove(StoredItemKey.languageCode)
        LocalizedStringResolver.clearCache()
    }

    // MARK: - Queries

    @Test
    fun `queried titles filter by a trimmed case insensitive search term`() {
        assertEquals(listOf("France (+33)"), RegionMenuReducer.State(searchQuery = " FRA ").queriedRegionTitles)
        assertEquals(4, RegionMenuReducer.State(searchQuery = "").queriedRegionTitles?.size)
    }

    @Test
    fun `queried titles are nil when nothing matches`() {
        assertNull(RegionMenuReducer.State(searchQuery = "zzz").queriedRegionTitles)
    }

    @Test
    fun `the selected title is the name first form of the selected region`() {
        assertEquals("Germany (+49)", RegionMenuReducer.State(selectedRegionCode = "DE").selectedRegionTitle)
    }

    @Test
    fun `the header is capitalized in English`() {
        assertEquals("Select Calling Code", RegionMenuReducer.State().headerLabelText)
    }

    // MARK: - Selection

    @Test
    fun `selecting a title records its region code and schedules dismissal`() {
        val result =
            reducer.reduce(
                RegionMenuReducer.State(isPresented = true),
                RegionMenuReducer.Action.SelectedRegionTitleChanged("France (+33)"),
            )

        assertEquals("FR", result.state.selectedRegionCode)
        assertNotNull(result.effect)
    }

    @Test
    fun `selecting an unknown title clears the region code`() {
        val result =
            reducer.reduce(
                RegionMenuReducer.State(selectedRegionCode = "US"),
                RegionMenuReducer.Action.SelectedRegionTitleChanged("Atlantis (+0)"),
            )

        assertEquals("", result.state.selectedRegionCode)
    }

    @Test
    fun `presentation changes apply directly through the run effect`() {
        val presented = reducer.reduce(RegionMenuReducer.State(), RegionMenuReducer.Action.RunIsPresentedEffect(true))
        val dismissed = reducer.reduce(presented.state, RegionMenuReducer.Action.RunIsPresentedEffect(false))

        assertEquals(true, presented.state.isPresented)
        assertEquals(false, dismissed.state.isPresented)
    }
}
