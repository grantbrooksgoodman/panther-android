//
//  RegionDetailServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.services

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.services.RegionDetailService.QueryStrategy
import us.neotechnica.panther.modules.common.services.RegionDetailService.RegionTitleFormat
import us.neotechnica.panther.modules.localization.models.LocalizedStringKey
import us.neotechnica.panther.subsystem.modules.foundation.models.StoredItemKey
import us.neotechnica.panther.subsystem.modules.foundation.models.languageCode
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.foundation.services.RuntimeStorage
import us.neotechnica.panther.subsystem.modules.localization.models.localized

class RegionDetailServiceTest {
    // MARK: - Setup

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

        CoreUtilities.setLanguageCode("en")
        RegionDetailService.clearCache()
    }

    @After
    fun tearDown() {
        CommonPropertyLists.clearCache()
        RegionDetailService.clearCache()
        RuntimeStorage.remove(StoredItemKey.languageCode)
    }

    // MARK: - Region Titles

    @Test
    fun `region titles carry the calling code without a flag`() {
        assertEquals(listOf("+49 (Germany)"), RegionDetailService.regionTitles(QueryStrategy.RegionCode("DE")))
        assertEquals(
            listOf("Germany (+49)"),
            RegionDetailService.regionTitles(
                QueryStrategy.RegionCode("DE"),
                titleFormat = RegionTitleFormat.REGION_NAME_FIRST,
            ),
        )
    }

    @Test
    fun `a calling code shared by several regions resolves to Multiple`() {
        assertEquals(listOf("+1 (Multiple)"), RegionDetailService.regionTitles(QueryStrategy.CallingCode("1")))
        assertEquals(LocalizedStringKey.Multiple.localized(), RegionDetailService.regionCode(QueryStrategy.CallingCode("1")))
        assertEquals("FR", RegionDetailService.regionCode(QueryStrategy.CallingCode("33")))
    }

    @Test
    fun `search terms filter the name-first titles and a blank term matches every title`() {
        assertEquals(listOf("France (+33)"), RegionDetailService.regionTitles(QueryStrategy.SearchTerm(" fra ")))
        assertNull(RegionDetailService.regionTitles(QueryStrategy.SearchTerm("zzz")))
        assertEquals(4, RegionDetailService.regionTitles(QueryStrategy.SearchTerm(""))?.size)
    }

    @Test
    fun `a region title resolves back to its region code`() {
        assertEquals("DE", RegionDetailService.regionCode(QueryStrategy.RegionTitle("Germany (+49)")))
        assertEquals("DE", RegionDetailService.regionCode(QueryStrategy.RegionTitle("+49 (Germany)")))
    }

    // MARK: - Localized Region Names

    @Test
    fun `region names localize to the app language, not the device locale`() {
        CoreUtilities.setLanguageCode("es")
        RegionDetailService.clearCache()

        assertEquals("Alemania", RegionDetailService.localizedRegionName("DE"))
        assertTrue(RegionDetailService.regionTitles(QueryStrategy.RegionCode("DE"))?.first()?.contains("Alemania") == true)
    }

    @Test
    fun `an explicit language code overrides the app language`() {
        assertEquals("Allemagne", RegionDetailService.localizedRegionName("DE", languageCode = "fr"))
        assertEquals("Germany", RegionDetailService.localizedRegionName("DE"))
    }

    @Test
    fun `an unknown region code is returned unchanged`() {
        assertEquals("ZZ", RegionDetailService.localizedRegionName("ZZ"))
    }
}
