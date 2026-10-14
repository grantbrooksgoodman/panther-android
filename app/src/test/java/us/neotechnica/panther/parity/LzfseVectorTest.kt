//
//  LzfseVectorTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.parity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.lzfse.Lzfse
import java.util.Base64

/**
 * Validates [Lzfse.decode] byte for byte against vectors produced by
 * the reference LZFSE encoder across the uncompressed, LZVN, and
 * FSE-compressed (v2) block types.
 */
class LzfseVectorTest {
    @Test
    fun `decodes every Apple-produced LZFSE vector back to its plaintext`() {
        val resource =
            javaClass.getResourceAsStream("/parity/lzfse_vectors.json")
                ?: error("Missing lzfse_vectors.json test resource.")
        val vectors = Json.parseToJsonElement(resource.bufferedReader().readText()).jsonArray

        var count = 0
        for (element in vectors) {
            val vector = element.jsonObject
            val name = vector.getValue("name").jsonPrimitive.content
            val plaintext = Base64.getDecoder().decode(vector.getValue("plaintext_base64").jsonPrimitive.content)
            val compressed = Base64.getDecoder().decode(vector.getValue("lzfse_base64").jsonPrimitive.content)

            assertArrayEquals("vector '$name'", plaintext, Lzfse.decode(compressed))
            count++
        }

        assert(count > 0) { "No LZFSE vectors were exercised." }
    }
}
