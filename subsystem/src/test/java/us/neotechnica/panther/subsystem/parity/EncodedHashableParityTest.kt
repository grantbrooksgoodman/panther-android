//
//  EncodedHashableParityTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.parity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.SwiftJSONEncoder
import java.util.Base64

class EncodedHashableParityTest {
    // MARK: - Tests

    @Test
    fun `encoded hash vectors match iOS byte for byte`() {
        val vectors = loadFixture("encoded_hash_vectors.json").jsonArray
        check(vectors.isNotEmpty())

        for (vector in vectors) {
            val entry = vector.jsonObject
            val factors =
                entry.getValue("factors").jsonArray.map {
                    it.jsonPrimitive.content
                }

            val expectedJSON = entry.getValue("json").jsonPrimitive.content
            val expectedBytes =
                Base64.getDecoder().decode(
                    entry.getValue("jsonBytesBase64").jsonPrimitive.content,
                )

            val expectedSHA256 = entry.getValue("sha256").jsonPrimitive.content

            assertEquals(
                "JSON mismatch for factors $factors",
                expectedJSON,
                SwiftJSONEncoder.encodeToString(factors),
            )

            assertArrayEquals(
                "Byte mismatch for factors $factors",
                expectedBytes,
                SwiftJSONEncoder.encode(factors),
            )

            assertEquals(
                "Hash mismatch for factors $factors",
                expectedSHA256,
                encodedHashOf(factors),
            )
        }
    }

    @Test
    fun `type fixture hashes match iOS`() {
        val fixture = loadFixture("type_hashes.json").jsonObject

        for (prefix in listOf("conversation", "message", "phoneNumber", "user")) {
            val expectedHash =
                fixture
                    .getValue("${prefix}Hash")
                    .jsonPrimitive
                    .content

            val factors =
                fixture
                    .getValue("${prefix}HashFactorsSorted")
                    .jsonArray
                    .map { it.jsonPrimitive.content }

            assertEquals(
                "Hash mismatch for $prefix",
                expectedHash,
                encodedHashOf(factors),
            )
        }
    }

    // MARK: - Auxiliary

    private fun loadFixture(name: String) =
        Json.parseToJsonElement(
            checkNotNull(javaClass.classLoader?.getResource("parity/$name")) {
                "Missing fixture parity/$name"
            }.readText(),
        )
}
