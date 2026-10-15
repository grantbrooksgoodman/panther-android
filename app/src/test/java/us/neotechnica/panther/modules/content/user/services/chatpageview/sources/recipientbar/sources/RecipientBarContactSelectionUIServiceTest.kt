//
//  RecipientBarContactSelectionUIServiceTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import us.neotechnica.panther.modules.content.user.extensions.mock
import us.neotechnica.panther.modules.content.user.services.chatpageview.sources.recipientbar.RecipientBarService
import us.neotechnica.panther.modules.networking.sources.support.MessageDecodeEnvironment
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHash
import us.neotechnica.panther.subsystem.modules.foundation.services.CoreUtilities
import us.neotechnica.panther.subsystem.modules.localization.models.LocalizationSource
import us.neotechnica.panther.subsystem.modules.localization.services.LocalizedStringResolver
import java.io.File

/**
 * Exercises the selected-contact views: the recipient cap,
 * de-duplication, mock removal, the highlighted trailing recipient
 * being replaced on select, highlighting, and the label representation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("ClassNaming")
class RecipientBarContactSelectionUIServiceTest {
    // MARK: - Setup

    private val environment = MessageDecodeEnvironment()
    private val service = RecipientBarService.contactSelectionUI

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        environment.install("recipient-bar-selection-test")

        LocalizedStringResolver.initializeForTesting { source ->
            val root = if (source == LocalizationSource.APP) "src/main/assets" else "../subsystem/src/main/assets"
            val json = Json.parseToJsonElement(File(root, source.assetName).readText()).jsonObject
            json.mapValues { (_, languages) ->
                languages.jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            }
        }

        CoreUtilities.setLanguageCode("en")
        RecipientBarService.reset()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        RecipientBarService.reset()
        LocalizedStringResolver.clearCache()
    }

    // MARK: - Selection Cap

    @Test
    fun `selecting more than the maximum keeps only the first ten`() {
        (1..11).forEach { service.selectContactPair(contactPair(it)) }
        assertEquals(10, service.selectedContactPairs.value.size)
    }

    // MARK: - De-duplication

    @Test
    fun `selecting the same contact pair twice keeps one and clears the text field`() {
        val pair = contactPair(1)
        service.selectContactPair(pair)
        RecipientBarService.layout.setTextFieldText("Ada")
        service.selectContactPair(pair)

        assertEquals(1, service.selectedContactPairs.value.size)
        assertEquals("", RecipientBarService.layout.textFieldText.value)
    }

    // MARK: - Mock De-selection

    @Test
    fun `selecting a registered recipient removes any mock recipients`() {
        service.selectContactPair(ContactPair.mock(withName = "Unknown"))
        service.selectContactPair(contactPair(1))

        val selected = service.selectedContactPairs.value
        assertEquals(1, selected.size)
        assertEquals("User1", selected.single().contact.firstName)
    }

    @Test
    fun `a mock selection leaves the input bar placeholder untouched`() {
        service.selectContactPair(ContactPair.mock(withName = "Unknown"))
        assertEquals("", RecipientBarService.layout.inputBarPlaceholder.value)

        service.selectContactPair(contactPair(1))
        assertEquals(" New Message", RecipientBarService.layout.inputBarPlaceholder.value)
    }

    // MARK: - Highlighted Replacement

    @Test
    fun `a highlighted trailing recipient is replaced by the next selection`() {
        val first = contactPair(1)
        val second = contactPair(2)
        service.selectContactPair(first)
        service.selectContactPair(second)
        service.toggleIsHighlighted(viewID = second.contact.encodedHash)

        service.selectContactPair(contactPair(3))

        assertEquals(listOf("User1", "User3"), service.selectedContactPairs.value.map { it.contact.firstName })
        assertEquals(null, service.highlightedViewID.value)
    }

    // MARK: - Highlighting

    @Test
    fun `highlighting toggles, focuses the text field, and deselection removes the recipient`() {
        val pair = contactPair(1)
        service.selectContactPair(pair)
        val viewID = pair.contact.encodedHash

        service.toggleIsHighlighted(viewID)
        assertTrue(service.isHighlighted(viewID))

        service.toggleIsHighlighted(viewID)
        assertFalse(service.isHighlighted(viewID))

        service.toggleIsHighlighted(viewID)
        service.deselectContactPair(withViewID = viewID)
        assertTrue(service.selectedContactPairs.value.isEmpty())
        assertFalse(service.isHighlighted(viewID))
    }

    // MARK: - Label Representation

    @Test
    fun `the label representation toggles as a visual state`() {
        service.toggleLabelRepresentation(on = true)
        assertTrue(service.isLabelRepresentationOn.value)

        service.toggleLabelRepresentation(on = false)
        assertFalse(service.isLabelRepresentationOn.value)
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
