//
//  LocalAudioFilePathTest.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.message.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import us.neotechnica.panther.modules.common.models.AudioFileExtension
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import java.util.Date

/**
 * Verifies that [LocalAudioFilePath.from] derives the translated-output
 * path from the hosting key of the *resolved* translation, rather than the
 * first of a message's translation references.
 *
 * A group audio message carries one reference per participant language, so
 * its first reference is frequently not the one being displayed – picking
 * it blindly points the download at a directory that does not exist.
 */
class LocalAudioFilePathTest {
    @Test
    fun fromPicksHostingKeyMatchingResolvedTranslationLanguagePair() {
        val message = audioMessage(IDEMPOTENT_SPANISH_HOSTING_KEY, ARCHIVED_SPANISH_KOREAN_HOSTING_KEY)
        val path = LocalAudioFilePath.from(message, translation(LanguagePair(from = "es", to = "ko")))

        assertEquals("audioTranslations/$ARCHIVED_SPANISH_KOREAN_HOSTING_KEY/ko-output.m4a", path?.outputFilePathString)
    }

    @Test
    fun fromIgnoresReferenceOrderWhenMatchingLanguagePair() {
        val message = audioMessage(ARCHIVED_SPANISH_KOREAN_HOSTING_KEY, IDEMPOTENT_SPANISH_HOSTING_KEY)
        val path = LocalAudioFilePath.from(message, translation(LanguagePair(from = "es", to = "ko")))

        assertEquals("audioTranslations/$ARCHIVED_SPANISH_KOREAN_HOSTING_KEY/ko-output.m4a", path?.outputFilePathString)
    }

    @Test
    fun fromReturnsNullWhenNoReferenceMatchesLanguagePair() {
        val message = audioMessage(IDEMPOTENT_SPANISH_HOSTING_KEY)
        assertNull(LocalAudioFilePath.from(message, translation(LanguagePair(from = "es", to = "ko"))))
    }

    // MARK: - Auxiliary

    private fun audioMessage(vararg hostingKeys: String): Message =
        Message(
            id = MESSAGE_ID,
            fromAccountID = "sender",
            contentType = HostedContentType.Audio(AudioFileExtension.M4A),
            translationReferences = hostingKeys.map { TranslationReference(it) },
            readReceipts = null,
            sentDate = Date(0),
        )

    private fun translation(languagePair: LanguagePair): Translation =
        Translation(input = TranslationInput("Hola a todos"), output = "모두 안녕하세요", languagePair = languagePair)

    private companion object {
        const val MESSAGE_ID = "-P1wjn9JwyLxoSigkx7l"
        const val IDEMPOTENT_SPANISH_HOSTING_KEY = "IDEM es | SG9sYSBhIHRvZG9z"
        const val ARCHIVED_SPANISH_KOREAN_HOSTING_KEY = "es-ko | abc123def456"
    }
}
