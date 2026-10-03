//
//  RecipientBarContactSelectionUIServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman on 03/10/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.neotechnica.panther.modules.common.models.Contact
import us.neotechnica.panther.modules.common.models.ContactPair
import us.neotechnica.panther.modules.common.models.NumberPair
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.extensions.mock
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.services.Persistent

class RecipientBarContactSelectionUIServiceTest {
    private val holder = RecipientBarContactSelectionUIService

    @Before
    fun setUp() {
        Persistent.initializeForTesting()
        holder.reset()
    }

    // MARK: - Selection Cap

    @Test
    fun `selecting more than the maximum keeps only the first ten`() {
        (1..11).forEach { holder.selectContactPair(contactPair(it)) }
        assertEquals(10, holder.selectedContactPairs.value.size)
    }

    // MARK: - De-duplication

    @Test
    fun `selecting the same contact pair twice keeps one`() {
        val pair = contactPair(1)
        holder.selectContactPair(pair)
        holder.selectContactPair(pair)
        assertEquals(1, holder.selectedContactPairs.value.size)
    }

    // MARK: - Mock De-selection

    @Test
    fun `selecting a registered recipient removes any mock recipients`() {
        holder.selectContactPair(ContactPair.mock(withName = "Unknown"))
        holder.selectContactPair(contactPair(1))
        val selected = holder.selectedContactPairs.value
        assertEquals(1, selected.size)
        assertEquals("User1", selected.single().contact.firstName)
    }

    // MARK: - Highlighting and Backspace

    @Test
    fun `highlighting toggles and deselection removes the recipient`() {
        val pair = contactPair(1)
        holder.selectContactPair(pair)
        val viewID = pair.contact.encodedHash

        holder.toggleIsHighlighted(viewID)
        assertTrue(holder.isHighlighted(viewID))

        holder.toggleIsHighlighted(viewID)
        assertFalse(holder.isHighlighted(viewID))

        holder.toggleIsHighlighted(viewID)
        holder.deselectContactPair(viewID)
        assertTrue(holder.selectedContactPairs.value.isEmpty())
        assertFalse(holder.isHighlighted(viewID))
    }

    // MARK: - Auxiliary

    private fun contactPair(index: Int): ContactPair {
        val phoneNumber =
            PhoneNumber(
                callingCode = "1",
                nationalNumberString = "555000${index.toString().padStart(4, '0')}",
                regionCode = "US",
                label = null,
                internalFormattedString = null,
            )
        return ContactPair(
            contact =
                Contact(
                    id = "contact-$index",
                    firstName = "User$index",
                    lastName = "",
                    phoneNumbers = listOf(phoneNumber),
                    imageData = null,
                ),
            numberPairs = listOf(NumberPair(phoneNumber = phoneNumber, userIDs = listOf("user-$index"))),
        )
    }
}
