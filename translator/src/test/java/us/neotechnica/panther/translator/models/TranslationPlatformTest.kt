//
//  TranslationPlatformTest.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.translator.models

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationPlatformTest {
    // MARK: - Tests

    @Test
    fun `query encoding leaves the query-allowed characters unescaped`() {
        assertEquals(
            "a%20b!$&'()*+,-./:;=?@_~%C3%A9",
            TranslationPlatform.percentEncoded("a b!$&'()*+,-./:;=?@_~é"),
        )
    }

    @Test
    fun `the API fast path also escapes the reserved query delimiters`() {
        assertEquals(
            "a%2Bb%26c%3Dd%3F",
            TranslationPlatform.percentEncoded(
                "a+b&c=d?",
                allowedCharacters = TranslationPlatform.URL_QUERY_ALLOWED_CHARACTERS.filterNot { it in "+&=?" },
            ),
        )
    }
}
