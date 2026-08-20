//
//  TimestampParityTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.parity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import us.neotechnica.panther.subsystem.modules.foundation.services.TimestampDateFormatter
import java.util.Date

class TimestampParityTest {
    // MARK: - Properties

    private val formatter = TimestampDateFormatter()

    // MARK: - Tests

    @Test
    fun `formatted timestamps match iOS byte for byte`() {
        val vectors = loadFixture("timestamp_vectors.json").jsonArray
        check(vectors.isNotEmpty())

        for (vector in vectors) {
            val entry = vector.jsonObject
            val epochMillis =
                entry
                    .getValue("epochMillis")
                    .jsonPrimitive.content
                    .toLong()
            val expectedFormatted = entry.getValue("formatted").jsonPrimitive.content
            val expectedReparsed =
                entry
                    .getValue("reparsedEpochMillis")
                    .jsonPrimitive
                    .content
                    .toLong()

            val formatted = formatter.format(Date(epochMillis))
            assertEquals(
                "Format mismatch for epoch $epochMillis",
                expectedFormatted,
                formatted,
            )

            assertEquals(
                "Reparse mismatch for \"$formatted\"",
                expectedReparsed,
                formatter.parse(formatted)?.time,
            )
        }
    }

    @Test
    fun `parse tolerance matches iOS`() {
        val vectors = loadFixture("timestamp_parse_vectors.json").jsonArray
        check(vectors.isNotEmpty())

        for (vector in vectors) {
            val entry = vector.jsonObject
            val string = entry.getValue("string").jsonPrimitive.content
            val expectedEpochMillis = entry["epochMillis"]?.jsonPrimitive?.longOrNull

            if (expectedEpochMillis == null) {
                assertNull(
                    "Expected \"$string\" to be rejected",
                    formatter.parse(string),
                )
            } else {
                assertEquals(
                    "Parse mismatch for \"$string\"",
                    expectedEpochMillis,
                    formatter.parse(string)?.time,
                )
            }
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
