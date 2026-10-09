//
//  Translation+Serializable.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.networking.modules.translation.serializable

import us.neotechnica.panther.networking.Networking
import us.neotechnica.panther.networking.modules.common.extensions.Networking
import us.neotechnica.panther.networking.modules.translation.extensions.base64Decoded
import us.neotechnica.panther.networking.modules.translation.extensions.decodedTranslationComponents
import us.neotechnica.panther.networking.modules.translation.extensions.reference
import us.neotechnica.panther.networking.modules.translation.extensions.sanitized
import us.neotechnica.panther.networking.modules.translation.models.TranslationReference
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.translator.Translator
import us.neotechnica.panther.translator.models.Translation
import us.neotechnica.panther.translator.models.TranslationInput
import us.neotechnica.panther.translator.services.LocalTranslationArchiver

/**
 * The serialized representation of this translation.
 *
 * Encoding produces a compact [TranslationReference]; decode it
 * back into a translation with [Translation.Companion.from].
 */
val Translation.encoded: TranslationReference
    get() = reference

/**
 * Returns `true` for all translation references, because validity
 * is determined during decoding rather than upfront inspection.
 */
@Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
fun Translation.Companion.canDecode(data: TranslationReference): Boolean = true

/**
 * Creates a translation by decoding from the specified reference.
 *
 * The decoding strategy depends on the reference type:
 *
 * - **Archived with inline value:** Decoded directly without a
 *   network request.
 * - **Archived without inline value:** Resolved from the local
 *   archive first, falling back to the hosted archive if needed.
 * - **Idempotent:** Decoded directly from the Base64-encoded
 *   input. No network request is required because the input and
 *   output languages are the same.
 *
 * Successfully decoded translations are added to the local
 * archive for future lookups when their input and output differ.
 *
 * @param data The translation reference to decode.
 *
 * @return The decoded translation.
 *
 * @throws Exception if decoding fails.
 */
suspend fun Translation.Companion.from(data: TranslationReference): Translation {
    val localTranslationArchiver = Translator.config.archiverDelegate ?: LocalTranslationArchiver

    fun addToArchive(translation: Translation) {
        if (translation.input.value == translation.output) return
        localTranslationArchiver.addValue(translation)
    }

    when (val type = data.type) {
        is TranslationReference.Type.Archived -> {
            val value = type.value
            if (value != null) {
                val components =
                    value.decodedTranslationComponents ?: throw Exception.Networking.decodingFailed(
                        data,
                        ExceptionMetadata(Translation),
                    )

                val decoded =
                    Translation(
                        input = TranslationInput(components.first),
                        output = components.second,
                        languagePair = data.languagePair,
                    )

                addToArchive(decoded)
                return decoded
            }

            localTranslationArchiver
                .getValue(
                    inputValueEncodedHash = type.hash,
                    languagePair = data.languagePair,
                )?.let { return it }

            val translation =
                Networking
                    .config
                    .hostedTranslationDelegate
                    .findArchivedTranslation(
                        inputValueEncodedHash = type.hash,
                        languagePair = data.languagePair,
                    )

            addToArchive(translation)
            return translation
        }

        is TranslationReference.Type.Idempotent -> {
            val decoded =
                Translation(
                    input = TranslationInput(type.encodedValue.base64Decoded),
                    output = type.encodedValue.base64Decoded.sanitized,
                    languagePair = data.languagePair,
                )

            addToArchive(decoded)
            return decoded
        }
    }
}
