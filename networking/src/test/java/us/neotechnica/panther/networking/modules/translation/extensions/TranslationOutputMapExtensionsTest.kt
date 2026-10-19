//
//  TranslationOutputMapExtensionsTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.TranslatedLabelStringCollection
import us.neotechnica.panther.networking.modules.translation.models.TranslationInputMap
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Exercises the label-string lookup: a resolved entry wins, a missing
 * one falls back to the collection's source text, and an unknown key
 * to its identifier.
 */
class TranslationOutputMapExtensionsTest {
    // MARK: - Setup

    private object Strings : TranslatedLabelStrings {
        val backButtonText = TranslatedLabelStringCollection("authCodePageView.backButtonText")
        val titleLabelText = TranslatedLabelStringCollection("authCodePageView.titleLabelText")

        override val keyPairs: List<TranslationInputMap> =
            listOf(
                TranslationInputMap(backButtonText, TranslationInput("Back")),
                TranslationInputMap(titleLabelText, TranslationInput("Enter Code")),
            )
    }

    // MARK: - Tests

    @Test
    fun `a partial map falls back to the source text rather than the identifier`() {
        val partial = listOf(TranslationOutputMap(Strings.titleLabelText, "Introduce el código"))

        assertEquals("Introduce el código", partial.value(Strings.titleLabelText, Strings))
        assertEquals("Back", partial.value(Strings.backButtonText, Strings))
    }

    @Test
    fun `a key outside the collection falls back to its identifier`() {
        val unknown = TranslatedLabelStringCollection("authCodePageView.unknown")

        assertEquals("authCodePageView.unknown", emptyList<TranslationOutputMap>().value(unknown, Strings))
    }
}
