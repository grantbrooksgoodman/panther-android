//
//  HostedTranslationArchiver.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.models.NetworkPath
import us.neotechnica.panther.networking.modules.translation.extensions.decodedTranslationComponents
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.networking.modules.translation.models.TranslationValidator
import us.neotechnica.panther.subsystem.modules.foundation.interfaces.encodedHashOf
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.LanguagePair
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Reads and writes translations in the hosted RTDB archive, keyed
 * `translations/<from>-<to>/<encodedHash(input)>`.
 *
 * **Note:** the iOS original keeps a whole-tree snapshot (making
 * absence authoritative and enabling transitive derivation of new
 * pairs). This Phase 4 port performs direct per-hash reads; the
 * snapshot and derivation are a deferred optimization.
 */
internal class HostedTranslationArchiver {
    // MARK: - Dependencies

    private val database get() = Networking.config.databaseDelegate

    // MARK: - Add to Hosted Archive

    suspend fun addToHostedArchive(translation: Translation) {
        TranslationValidator.validate(sender = this, translation = translation)

        val entry =
            hostedArchiveEntry(translation) ?: throw Exception(
                "Translation language pair is idempotent; ineligible for hosted archive.",
                metadata = ExceptionMetadata(this),
            )

        database.commit(mapOf(entry.first to entry.second))
    }

    fun hostedArchiveEntry(translation: Translation): Pair<String, Any>? {
        try {
            TranslationValidator.validate(sender = this, translation = translation)
        } catch (_: Exception) {
            return null
        }

        if (translation.languagePair.isIdempotent) return null

        val reference = TranslationReference.from(translation)
        val referenceValue = reference.type.value ?: return null
        val key =
            listOf(
                NetworkPath.translations.rawValue,
                translation.languagePair.string,
                reference.type.key,
            ).joinToString("/")

        return key to referenceValue
    }

    // MARK: - Find Archived Translation

    suspend fun findArchivedTranslation(
        input: TranslationInput,
        languagePair: LanguagePair,
    ): Translation =
        findArchivedTranslation(
            inputValueEncodedHash = encodedHashOf(listOf(input.value)),
            languagePair = languagePair,
        )

    suspend fun findArchivedTranslation(
        inputValueEncodedHash: String,
        languagePair: LanguagePair,
    ): Translation {
        TranslationValidator.validate(sender = this, languagePair = languagePair)

        val path = "${NetworkPath.translations.rawValue}/${languagePair.string}/$inputValueEncodedHash"
        val raw: String = database.getValues(path)

        val components =
            raw.decodedTranslationComponents ?: throw Exception(
                "Failed to decode archived translation.",
                userInfo = mapOf("Path" to path),
                metadata = ExceptionMetadata(this),
            )

        return Translation(
            input = TranslationInput(components.first),
            output = components.second,
            languagePair = languagePair,
        )
    }

    // MARK: - Remove Archived Translation

    suspend fun removeArchivedTranslation(
        input: TranslationInput,
        languagePair: LanguagePair,
    ) {
        val path =
            listOf(
                Networking.config.environment.shortString,
                NetworkPath.translations.rawValue,
                languagePair.string,
            ).joinToString("/")

        database.updateChildValues(
            key = path,
            data = mapOf(encodedHashOf(listOf(input.value)) to null),
            prependingEnvironment = false,
        )
    }
}
