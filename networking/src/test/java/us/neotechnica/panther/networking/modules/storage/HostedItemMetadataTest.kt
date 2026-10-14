//
//  HostedItemMetadataTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.storage

import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.networking.modules.storage.models.HostedItemMetadata

class HostedItemMetadataTest {
    @Test
    fun `storage metadata carries the item's own values`() {
        val metadata =
            HostedItemMetadata(
                "media/item.jpg",
                cacheControl = "no-cache",
                contentType = "image/jpeg",
                customValues = mapOf("key" to "value"),
            ).asStorageMetadata()

        assertEquals("no-cache", metadata.cacheControl)
        assertEquals("image/jpeg", metadata.contentType)
        assertEquals(setOf("key"), metadata.customMetadataKeys)
    }
}
