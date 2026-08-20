//
//  TranslationReferenceParityTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.parity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.translation.extensions.alphaEncoded
import us.neotechnica.panther.networking.modules.translation.extensions.decodedTranslationComponents
import us.neotechnica.panther.networking.modules.translation.extensions.encodedHash
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Verifies the hosted translation archive wire format byte-for-byte
 * against the golden vectors generated from the iOS sources, so a
 * translation Android writes is readable by iOS and vice versa.
 *
 * The idempotent vectors are skipped: their reference key is a
 * `android.util.Base64` encoding that is unavailable to JVM unit tests.
 */
class TranslationReferenceParityTest {
    @Test
    fun archivedReferenceVectorsMatchGoldenWireFormat() {
        var verified = 0

        for (element in loadVectors()) {
            val vector = element.jsonObject

            fun string(key: String): String = vector.getValue(key).jsonPrimitive.content
            if (vector.getValue("isIdempotent").jsonPrimitive.boolean) continue

            val input = string("input")
            val output = string("output")
            val languagePair = LanguagePair(from = string("from"), to = string("to"))

            assertEquals("alphaEncoded(input) for \"$input\"", string("inputAlphaEncoded"), input.alphaEncoded)
            assertEquals("alphaEncoded(output) for \"$output\"", string("outputAlphaEncoded"), output.alphaEncoded)
            assertEquals("encodedHash(input) for \"$input\"", string("inputEncodedHash"), input.encodedHash)

            val reference =
                TranslationReference.from(
                    Translation(input = TranslationInput(input), output = output, languagePair = languagePair),
                )
            assertEquals("archived value for \"$input\"", string("archivedValue"), reference.type.value)
            assertEquals("reference key for \"$input\"", string("referenceKey"), reference.type.key)
            assertEquals("hosting key for \"$input\"", string("hostingKey"), reference.hostingKey)

            val pathSuffix = "${NetworkPath.translations.rawValue}/${languagePair.string}/${input.encodedHash}"
            assertEquals("archive path suffix for \"$input\"", string("archivePathSuffix"), pathSuffix)

            val decoded = string("archivedValue").decodedTranslationComponents
            assertEquals("decoded input for \"$input\"", input, decoded?.first)
            assertEquals("decoded output for \"$input\"", output, decoded?.second)

            verified += 1
        }

        assert(verified > 0) { "No archived translation vectors were verified." }
    }

    // MARK: - Auxiliary

    private fun loadVectors() =
        Json
            .parseToJsonElement(
                checkNotNull(javaClass.classLoader?.getResource("parity/translation_reference_vectors.json")) {
                    "Missing fixture parity/translation_reference_vectors.json"
                }.readText(),
            ).jsonArray
}
