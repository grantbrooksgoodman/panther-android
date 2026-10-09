//
//  FakeHostedTranslationDelegate.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.networking.support

import us.neotechnica.panther.designsystem.modules.alertkit.models.HUDConfig
import us.neotechnica.panther.networking.modules.translation.interfaces.HostedTranslationDelegate
import us.neotechnica.panther.networking.modules.translation.interfaces.TranslatedLabelStrings
import us.neotechnica.panther.networking.modules.translation.models.ArchiveStrategy
import us.neotechnica.panther.networking.modules.translation.models.TranslationOutputMap
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * A [HostedTranslationDelegate] for tests that serves archived
 * translations from a seeded table.
 *
 * Seed [archivedTranslations] with the hash and language pair a
 * reference resolves through. [translate] echoes its input unless
 * [translateOutput] is replaced; every other operation is unsupported.
 */
class FakeHostedTranslationDelegate : HostedTranslationDelegate {
    // MARK: - Properties

    /** The translations served by [findArchivedTranslation], keyed by hash and language pair. */
    val archivedTranslations = mutableMapOf<Pair<String, String>, Translation>()

    /** The hashes and language pairs looked up so far. */
    val lookups = mutableListOf<Pair<String, String>>()

    /** The inputs and language pairs passed to [translate] so far. */
    val translateRequests = mutableListOf<Pair<TranslationInput, LanguagePair>>()

    /** Produces the output [translate] returns; echoes the input by default. */
    var translateOutput: (TranslationInput, LanguagePair) -> String = { input, _ -> input.value }

    // MARK: - HostedTranslationDelegate

    override suspend fun findArchivedTranslation(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation {
        val key = inputValueEncodedHash to languagePair.string
        lookups.add(key)
        return archivedTranslations[key]
            ?: throw Exception(
                "No value exists at the specified key path.",
                metadata = ExceptionMetadata(this),
            )
    }

    override suspend fun getTranslations(
        inputs: List<TranslationInput>,
        languagePair: LanguagePair,
        hudConfig: HUDConfig?,
    ): List<Translation> = error("getTranslations is not supported by FakeHostedTranslationDelegate.")

    override fun hostedArchiveEntry(translation: Translation): Pair<String, Any>? = null

    override suspend fun resolve(strings: TranslatedLabelStrings): List<TranslationOutputMap> =
        error("resolve is not supported by FakeHostedTranslationDelegate.")

    override suspend fun translate(
        input: TranslationInput,
        languagePair: LanguagePair,
        hudConfig: HUDConfig?,
        archiveStrategy: ArchiveStrategy,
    ): Translation {
        translateRequests.add(input to languagePair)
        return Translation(input, output = translateOutput(input, languagePair), languagePair = languagePair)
    }

    // MARK: - Seeding

    /** Serves [translation] for the hash and language pair of its reference. */
    fun seed(
        inputValueEncodedHash: String,
        translation: Translation,
    ) {
        archivedTranslations[inputValueEncodedHash to translation.languagePair.string] = translation
    }
}
