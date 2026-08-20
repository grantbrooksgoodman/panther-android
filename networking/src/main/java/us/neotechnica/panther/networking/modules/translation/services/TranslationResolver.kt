//
//  TranslationResolver.kt
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.services

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.base64Decoded
import us.neotechnica.panther.networking.modules.translation.extensions.decodedTranslationComponents
import us.neotechnica.panther.networking.modules.translation.extensions.sanitized
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput

/**
 * Resolves a [TranslationReference] back into a [Translation].
 *
 * This is the decode half of the iOS `Translation: Serializable`
 * conformance (deferred in Phase 4): an inline archived value decodes
 * directly, a hash-only archived reference resolves through the hosted
 * archive, and an idempotent reference decodes its Base64 input.
 */
object TranslationResolver {
    /**
     * Resolves [reference] into a [Translation].
     *
     * @throws Exception if an inline value cannot be decoded.
     */
    suspend fun resolve(reference: TranslationReference): Translation {
        val languagePair = reference.languagePair
        return when (val type = reference.type) {
            is TranslationReference.Type.Archived -> {
                val value = type.value
                if (value != null) {
                    val components =
                        value.decodedTranslationComponents ?: throw Exception(
                            "Failed to decode inline translation value.",
                            metadata = ExceptionMetadata(this),
                        )
                    Translation(
                        input = TranslationInput(components.first),
                        output = components.second,
                        languagePair = languagePair,
                    )
                } else {
                    Networking.config.hostedTranslationDelegate.findArchivedTranslation(type.hash, languagePair)
                }
            }

            is TranslationReference.Type.Idempotent -> {
                val decoded = type.encodedValue.base64Decoded
                Translation(
                    input = TranslationInput(decoded),
                    output = decoded.sanitized,
                    languagePair = languagePair,
                )
            }
        }
    }
}
