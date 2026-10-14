//
//  RecipientBarTableViewServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.models.QueriedContactPairCache
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

/**
 * Exercises the suggestions table: sectioning by last-name letter with
 * the `#` section last, the `0` query showing every contact, filtering,
 * and hiding the table on a blank query.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipientBarTableViewServiceTest {
    // MARK: - Setup

    private val service =
        RecipientBarTableViewService(
            knownContactPairsProvider = {
                listOf(
                    contactPair("1", "Ada", "Lovelace", "5550000001"),
                    contactPair("2", "Grace", "Hopper", "5550000002"),
                    contactPair("3", "Linus", "Torvalds", "5550000003"),
                    contactPair("4", "", "", "5550000004"),
                )
            },
            conversationsProvider = { null },
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        Persistent.initializeForTesting()
        QueriedContactPairCache.clearCache()
        QueriedContactPairCache.canWriteToCache = false
        RecipientBarService.reset()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        QueriedContactPairCache.clearCache()
        QueriedContactPairCache.canWriteToCache = false
        RecipientBarService.reset()
    }

    // MARK: - Sections

    @Test
    fun `a zero query shows every contact in letter sections with the number section last`() =
        runBlocking {
            service.resolveContactPairs()
            service.setQuery("0")

            val sections = service.sections.value
            assertTrue(service.isTableViewVisible.value)
            assertEquals(listOf("H", "L", "T", "#"), sections.map { it.letter })
            assertEquals(listOf("Hopper"), sections.first().contactPairs.map { it.contact.lastName })
        }

    @Test
    fun `a query filters the suggestions by name`() =
        runBlocking {
            service.resolveContactPairs()
            service.setQuery("ada")

            val sections = service.sections.value
            assertEquals(listOf("L"), sections.map { it.letter })
            assertEquals(listOf("Ada"), sections.single().contactPairs.map { it.contact.firstName })
        }

    @Test
    fun `a blank query hides the table and reloads the full set`() =
        runBlocking {
            service.resolveContactPairs()
            service.setQuery("ada")
            service.setQuery(" ")

            assertFalse(service.isTableViewVisible.value)
            assertEquals(4, service.sections.value.sumOf { it.contactPairs.size })
            assertTrue(QueriedContactPairCache.canWriteToCache)
        }

    // MARK: - Auxiliary

    private fun contactPair(
        id: String,
        firstName: String,
        lastName: String,
        nationalNumber: String,
    ): ContactPair {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = nationalNumber,
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        return ContactPair(
            contact = Contact(id = id, firstName = firstName, lastName = lastName, phoneNumbers = listOf(phoneNumber), imageData = null),
            numberPairs = listOf(NumberPair(phoneNumber = phoneNumber, userIDs = listOf("user-$id"))),
        )
    }
}
